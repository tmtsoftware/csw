/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.common.components.command

import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandResponseManager.{OverallFailure, OverallSuccess}
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.common.components.command.ComponentStateForCommand.{longRunningCmdCompleted, _}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.{PekkoLocation, TrackingEvent}
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse.*
import csw.params.commands.{CommandIssue, ControlCommand, Setup}
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime

import cps.*
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class McsAssemblyComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  private implicit val timeout: Timeout     = 10.seconds
  private implicit val ec: ExecutionContext = ctx.executionContext
  private var hcdComponent: CommandService  = _
  private val assemblyPrefix                = prefix

  import cswCtx._

  override def initialize(): Unit = {
    componentInfo.connections.headOption match {
      case Some(hcd) =>
        cswCtx.locationService.resolve(hcd.of[PekkoLocation], 5.seconds).map {
          case Some(pekkoLocation) => hcdComponent = CommandServiceFactory.make(pekkoLocation)(ctx.system)
          case None                => throw new RuntimeException("Could not resolve hcd location, Initialization failure.")
        }
      case None => ()
    }
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ()

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = {
    controlCommand.commandName match {
      case `longRunning` => Accepted(runId)
      case `moveCmd`     => Accepted(runId)
      case `initCmd`     => Accepted(runId)
      case `invalidCmd`  => Invalid(runId, CommandIssue.OtherIssue("Invalid"))
      case _             => Invalid(runId, UnsupportedCommandIssue(controlCommand.commandName.name))
    }
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    controlCommand.commandName match {
      case `longRunning` =>
        processLongRunningCommand(runId, controlCommand)
        // Return response
        Started(runId)
      case `initCmd` =>
        Completed(runId)
      case `moveCmd` =>
        Completed(runId)
      case _ => // do nothing
        Completed(runId)
    }
  }

  private def processLongRunningCommand(prunId: Id, controlCommand: ControlCommand): Unit = {
    // Could be different components, can't actually submit parallel commands to an HCD
    val shortSetup  = Setup(assemblyPrefix, shortRunning, None)
    val mediumSetup = Setup(assemblyPrefix, mediumRunning, None)
    val longSetup   = Setup(assemblyPrefix, longRunning, None)

    async {
      // Start 3 commands, could be going to 3 different HCDs
      val longsubmit: Future[SubmitResponse] = hcdComponent.submit(longSetup)
      val shortsubmit                        = hcdComponent.submit(shortSetup)
      val medium                             = hcdComponent.submit(mediumSetup)

      // Execute code when long completes
      val longresult: Future[SubmitResponse] = hcdComponent.queryFinal(await(longsubmit).runId) map { sr =>
        currentStatePublisher
          .publish(CurrentState(assemblyPrefix, StateName("testStateName"), Set(choiceKey.set(longCmdCompleted))))
        sr
      }

      // Execute code when medium completes
      val mediumresult: Future[SubmitResponse] = hcdComponent.queryFinal(await(medium).runId) map { sr =>
        currentStatePublisher
          .publish(CurrentState(assemblyPrefix, StateName("testStateName"), Set(choiceKey.set(mediumCmdCompleted))))
        sr
      }

      // Execute code when short completes
      val shortresult: Future[SubmitResponse] = hcdComponent.queryFinal(await(shortsubmit).runId) map { sr =>
        currentStatePublisher
          .publish(CurrentState(assemblyPrefix, StateName("testStateName"), Set(choiceKey.set(shortCmdCompleted))))
        sr
      }

      // Execute some code when all three of the commands have completed
      commandResponseManager.queryFinalAll(shortresult, mediumresult, longresult).onComplete {
        case Success(response) =>
          currentStatePublisher.publish(
            CurrentState(controlCommand.source, StateName("testStateName"), Set(choiceKey.set(longRunningCmdCompleted)))
          )
          response match {
            case OverallSuccess(_) =>
              // update parent with a positive result
              commandResponseManager.updateCommand(Completed(prunId))
            case OverallFailure(responses) =>
              // update parent with an Error including all the individual responses
              commandResponseManager.updateCommand(Error(prunId, s"$responses"))
          }
        case Failure(ex) =>
          commandResponseManager.updateCommand(Error(prunId, ex.toString))
      }
    }
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}
}
