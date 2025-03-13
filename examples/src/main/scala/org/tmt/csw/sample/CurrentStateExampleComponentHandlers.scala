/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package org.tmt.csw.sample

import org.apache.pekko.actor.Cancellable
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.stream.ThrottleMode
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandResponseManager.{OverallFailure, OverallSuccess}
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.ComponentType.HCD
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{ComponentId, TrackingEvent}
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.params.commands.*
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse.*
import csw.params.core.generics.KeyType
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.SystemEvent
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime

import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class CurrentStateExampleComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import ComponentStateForCommand.*
  import cswCtx.*

  private val log: Logger                                = loggerFactory.getLogger(ctx)
  private implicit val ec: ExecutionContext              = ctx.executionContext
  private implicit val actorSystem: ActorSystem[Nothing] = ctx.system
  private implicit val timeout: Timeout                  = 15.seconds
  private val filterHCDConnection                        = PekkoConnection(ComponentId(Prefix(Subsystem.CSW, "samplehcd"), HCD))
  val locationService: LocationService                   = cswCtx.locationService

  private val filterHCDLocation    = Await.result(locationService.resolve(filterHCDConnection, 5.seconds), 5.seconds)
  val hcdComponent: CommandService = CommandServiceFactory.make(filterHCDLocation.get)(ctx.system)

  private val longRunning       = Setup(seqPrefix, longRunningCmdToHcd, None)
  private val shortRunning      = Setup(seqPrefix, shorterHcdCmd, None)
  private val shortRunningError = Setup(seqPrefix, shorterHcdErrorCmd, None)

  override def initialize(): Unit = {
    log.info("Initializing Assembly Component TLA")
    // #currentStatePublisher
    // Publish the CurrentState using parameter set created using a sample Choice parameter
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    // #currentStatePublisher
  }

  override def onGoOffline(): Unit =
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit =
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

  def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    command.commandName match {
      case ComponentStateForCommand.`longRunning`  => Accepted(runId)
      case `mediumRunning`                         => Accepted(runId)
      case ComponentStateForCommand.`shortRunning` => Accepted(runId)
      case `immediateCmd` | `longRunningCmd` | `longRunningCmdToAsm` | `longRunningCmdToAsmComp` | `longRunningCmdToAsmInvalid` |
          `longRunningCmdToAsmCActor` | `cmdWithBigParameter` | `hcdCurrentStateCmd` | `matcherCmd` =>
        Accepted(runId)
      case `invalidCmd` =>
        Invalid(runId, OtherIssue("Invalid"))
      case _ =>
        Invalid(runId, OtherIssue("Testing: Received failure, will return Invalid."))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    processCommand(runId, controlCommand)
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {
    processCommandWithMatcher(controlCommand)
  }
  private def processCommandWithMatcher(controlCommand: ControlCommand): Unit =
    controlCommand.commandName match {
      case `hcdCurrentStateCmd` =>
        processCurrentStateOneway(controlCommand)
      case `matcherTimeoutCmd` => Thread.sleep(1000)
      case `matcherFailedCmd` =>
        Source(1 to 10)
          .map(i =>
            currentStatePublisher.publish(
              CurrentState(controlCommand.source, StateName("testStateName"), Set(KeyType.IntKey.make("encoder").set(i * 1)))
            )
          )
          .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
          .runWith(Sink.ignore)
      case _ =>
        Source(1 to 10)
          .map(i =>
            currentStatePublisher.publish(
              CurrentState(controlCommand.source, StateName("testStateName"), Set(KeyType.IntKey.make("encoder").set(i * 10)))
            )
          )
          .throttle(1, 100.millis, 1, ThrottleMode.Shaping)
          .runWith(Sink.ignore)
    }
  def processCommand(runId: Id, command: ControlCommand): SubmitResponse =
    command.commandName match {
      case `immediateCmd` =>
        Completed(runId)
      // #longRunning
      case `longRunningCmd` =>
        // A local assembly command that takes some time returning Started
        timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(2))) {
          // After time expires, send final update
          commandResponseManager.updateCommand(Completed(runId, Result(encoder.set(20))))
        }
        // Starts long-runing and returns started
        Started(runId)
      // #longRunning
      // #queryFinalAll
      case `longRunningCmdToAsm` =>
        hcdComponent.submitAndWait(longRunning).foreach {
          case _: Completed =>
            commandResponseManager.updateCommand(Completed(runId))
          case other =>
            // Handle some other response besides Completed
            commandResponseManager.updateCommand(other.withRunId(runId))
        }
        // Assembly starts long-running and returns started
        Started(runId)

      case `longRunningCmdToAsmComp` =>
        // In this case, assembly does not need to do anything until both commands complete
        // Could wait and return directly if commands are fast, but this is better
        val long    = hcdComponent.submitAndWait(longRunning)
        val shorter = hcdComponent.submitAndWait(shortRunning)

        commandResponseManager.queryFinalAll(long, shorter).onComplete {
          case Success(response) =>
            response match {
              case OverallSuccess(_) =>
                commandResponseManager.updateCommand(Completed(runId))
              case OverallFailure(responses) =>
                commandResponseManager.updateCommand(Error(runId, s"$responses"))
            }
          case Failure(ex) =>
            commandResponseManager.updateCommand(Error(runId, ex.toString))
        }
        Started(runId)
      // #queryFinalAll
      case `longRunningCmdToAsmInvalid` =>
        val long    = hcdComponent.submitAndWait(longRunning)
        val shorter = hcdComponent.submitAndWait(shortRunningError)

        commandResponseManager.queryFinalAll(long, shorter).onComplete {
          case Success(response) =>
            response match {
              case OverallSuccess(_) =>
                commandResponseManager.updateCommand(Completed(runId))
              case OverallFailure(_) =>
                commandResponseManager.updateCommand(Error(runId, s"ERROR"))
            }
          case Failure(ex) =>
            commandResponseManager.updateCommand(Error(runId, ex.toString))
        }
        Started(runId)

      case `cmdWithBigParameter` =>
        Completed(runId, Result(command.paramSet))

      case _ =>
        Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"${command.commandName.name}"))
    }

  override def onShutdown(): Unit = {
    currentStatePublisher.publish(CurrentState(filterAsmPrefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(500)
  }

  private def processCurrentStateOneway(controlCommand: ControlCommand): Unit = {
    val currentState = CurrentState(prefix, StateName("HCDState"), controlCommand.paramSet)
    cswCtx.currentStatePublisher.publish(currentState)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  // #onDiagnostic-mode
  // While dealing with mutable state, make sure you create a worker actor to avoid concurrency issues
  // For functionality demonstration, we have simply used a mutable variable without worker actor
  var diagModeCancellable: Option[Cancellable] = None

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {
    hint match {
      case "engineering" =>
        val event = SystemEvent(prefix, diagnosticDataEventName).add(diagnosticModeParam)
        diagModeCancellable.foreach(_.cancel()) // cancel previous diagnostic publishing
        diagModeCancellable = Some(eventService.defaultPublisher.publish(Some(event), startTime, 200.millis))
      case _ =>
    }
  }
  // #onDiagnostic-mode

  // #onOperations-mode
  override def onOperationsMode(): Unit = {
    diagModeCancellable.foreach(_.cancel())
  }
  // #onOperations-mode
}
