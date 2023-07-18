/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package example.tutorial.basic.sampleassembly

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandResponseManager.{OverallFailure, OverallSuccess}
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.event.api.scaladsl.EventSubscription
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.{PekkoLocation, ComponentId, ComponentType, LocationRemoved, LocationUpdated, TrackingEvent}
import csw.location.api.models.Connection.PekkoConnection
import csw.params.commands.CommandIssue.{MissingKeyIssue, ParameterValueOutOfRangeIssue, UnsupportedCommandIssue}
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandIssue, ControlCommand, Observe, Result, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.Id
import csw.params.events._
import csw.prefix.models.{Prefix, Subsystem}
import csw.time.core.models.UTCTime
import example.tutorial.basic.shared.SampleInfo._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Domain specific logic should be written in below handlers.
 * This handlers gets invoked when component receives messages/commands from other component/entity.
 * For example, if one component sends Submit(Setup(args)) command to SampleHcd,
 * This will be first validated in the supervisor and then forwarded to Component TLA which first invokes validateCommand hook
 * and if validation is successful, then onSubmit hook gets invoked.
 * You can find more information on this here : https://tmtsoftware.github.io/csw/framework.html
 */
//noinspection DuplicatedCode
//#resolve-hcd-and-create-commandservice
class SampleAssemblyHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
//#resolve-hcd-and-create-commandservice
  import cswCtx._
  private implicit val ec: ExecutionContextExecutor = ctx.executionContext
  // #resolve-hcd-and-create-commandservice
  private implicit val system: ActorSystem[Nothing] = ctx.system
  // #resolve-hcd-and-create-commandservice
  private implicit val timeout: Timeout     = 10.seconds
  private val log                           = loggerFactory.getLogger
  private val prefix: Prefix                = cswCtx.componentInfo.prefix
  private val hcdConnection                 = PekkoConnection(ComponentId(Prefix(Subsystem.ESW, "SampleHcd"), ComponentType.HCD))
  private var hcdLocation: PekkoLocation    = _
  private var hcdCS: Option[CommandService] = None

  // #initialize
  private var maybeEventSubscription: Option[EventSubscription] = None

  override def initialize(): Unit = {
    log.info(s"Assembly: $prefix initialize")
    maybeEventSubscription = Some(subscribeToHcd())
  }

  override def onShutdown(): Unit = {
    log.info(s"Assembly: $prefix is shutting down.")
  }
  // #initialize

  // #track-location
  // #resolve-hcd-and-create-commandservice
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {
    log.debug(s"onLocationTrackingEvent called: $trackingEvent")
    trackingEvent match {
      case LocationUpdated(location) =>
        hcdLocation = location.asInstanceOf[PekkoLocation]
        hcdCS = Some(CommandServiceFactory.make(location))
        onSetup(Id(), Setup(prefix, shortCommand, None))
      case LocationRemoved(connection) =>
        if (connection == hcdConnection) {
          hcdCS = None
        }
    }
  }
  // #resolve-hcd-and-create-commandservice
  // #track-location

  // #subscribe
  private val counterEventKey = EventKey(Prefix("CSW.samplehcd"), EventName("HcdCounter"))
  private val hcdCounterKey   = KeyType.IntKey.make("counter")

  private def processEvent(event: Event): Unit = {
    log.info(s"Assembly: $prefix received event: ${event.eventKey}")
    event match {
      case e: SystemEvent =>
        e.eventKey match {
          case `counterEventKey` =>
            val counter = e(hcdCounterKey).head
            log.info(s"Counter = $counter")
          case _ => log.warn("Unexpected event received.")
        }
      case _: ObserveEvent => log.warn("Unexpected ObserveEvent received.") // not expected
    }
  }

  private def subscribeToHcd(): EventSubscription = {
    log.info("Assembly: $prefix starting subscription.")
    eventService.defaultSubscriber.subscribeCallback(Set(counterEventKey), processEvent)
  }

  // noinspection ScalaUnusedSymbol
  private def unsubscribeHcd(): Unit = {
    log.info("Assembly: $prefix stopping subscription.")
    maybeEventSubscription.foreach(_.unsubscribe())
  }
  // #subscribe

  // #validate
  override def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse =
    command match {
      case setup: Setup =>
        setup.commandName match {
          case `sleep` =>
            validateSleep(runId, setup)
          case `immediateCommand` | `shortCommand` | `mediumCommand` | `longCommand` | `complexCommand` =>
            Accepted(runId)
          case _ =>
            Invalid(runId, UnsupportedCommandIssue(s"Command: ${setup.commandName.name} is not supported for sample Assembly."))
        }
      case _ =>
        Invalid(runId, UnsupportedCommandIssue(s"Command: ${command.commandName.name} is not supported for sample Assembly."))
    }

  private def validateSleep(runId: Id, setup: Setup): ValidateCommandResponse =
    if (setup.exists(sleepTimeKey)) {
      val sleepTime: Long = setup(sleepTimeKey).head
      if (sleepTime < maxSleep)
        Accepted(runId)
      else
        Invalid(runId, ParameterValueOutOfRangeIssue("sleepTime must be < 2000"))
    }
    else {
      Invalid(runId, MissingKeyIssue(s"required sleep command key: $sleepTimeKey is missing."))
    }
  // #validate

  // #submit-split
  override def onSubmit(runId: Id, command: ControlCommand): SubmitResponse =
    command match {
      case s: Setup => onSetup(runId, s)
      case _: Observe =>
        Invalid(runId, UnsupportedCommandIssue("Observe commands not supported"))
    }
  // #submit-split

  // #sending-command
  // #immediate-command
  private def onSetup(runId: Id, setup: Setup): SubmitResponse =
    setup.commandName match {
      case `immediateCommand` =>
        val localValue = 1000L
        // Assembly preforms a calculation or reads state information storing in a result
        Completed(runId, Result().add(resultKey.set(localValue)))
      // #immediate-command
      case `shortCommand` =>
        sleepHCD(runId, setup, shortSleepPeriod)
        Started(runId)

      case `mediumCommand` =>
        sleepHCD(runId, setup, mediumSleepPeriod)
        Started(runId)

      case `longCommand` =>
        sleepHCD(runId, setup, longSleepPeriod)
        Started(runId)
      // #queryF

      case `complexCommand` =>
        val medium = simpleHCD(runId, Setup(prefix, hcdSleep, setup.maybeObsId).add(setSleepTime(mediumSleepPeriod)))
        val long   = simpleHCD(runId, Setup(prefix, hcdSleep, setup.maybeObsId).add(setSleepTime(longSleepPeriod)))

        commandResponseManager
          .queryFinalAll(medium, long)
          .map {
            case OverallSuccess(_) =>
              // Don't care about individual responses with success
              commandResponseManager.updateCommand(Completed(runId))
            case OverallFailure(responses) =>
              // There must be at least one error
              val errors = responses.filter(isNegative)
              commandResponseManager.updateCommand(errors.head.withRunId(runId))
          }
          .recover(ex => commandResponseManager.updateCommand(Error(runId, ex.toString)))

        Started(runId)
      case `sleep` =>
        sleepHCD(runId, setup, setup(sleepTimeKey).head)
        Started(runId)
      case _ =>
        Invalid(runId, CommandIssue.UnsupportedCommandIssue(s"${setup.commandName.name}"))
    }

  private def simpleHCD(runId: Id, setup: Setup): Future[SubmitResponse] =
    hcdCS match {
      case Some(cs) =>
        cs.submitAndWait(setup)
      case None =>
        Future(Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId}"))
    }
  // #queryF

  // #updateCommand
  // #submitAndQueryFinal
  private def sleepHCD(runId: Id, setup: Setup, sleepTime: Long): Unit =
    hcdCS match {
      case Some(cs) =>
        val s = Setup(prefix, hcdSleep, None).add(setSleepTime(sleepTime))
        cs.submit(s).foreach {
          case started: Started =>
            // Can insert extra code during execution here
            cs.queryFinal(started.runId).foreach(sr => commandResponseManager.updateCommand(sr.withRunId(runId)))
          case other =>
            commandResponseManager.updateCommand(other.withRunId(runId))
        }
      case None =>
        commandResponseManager.updateCommand(
          Error(runId, s"A needed HCD is not available: ${hcdConnection.componentId} for $prefix")
        )
    }
  // #updateCommand
  // #submitAndQueryFinal
  // #sending-command

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {
    // do something on Diagnostic Mode
  }

  override def onOperationsMode(): Unit = {
    // do something on Operations Mode
  }

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

}
