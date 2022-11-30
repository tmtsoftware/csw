/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.common.components.framework

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models.{LocationRemoved, LocationUpdated, TrackingEvent}
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{Event, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

class SampleComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._

  val log: Logger                   = loggerFactory.getLogger(ctx)
  implicit val ec: ExecutionContext = ctx.executionContext

  import SampleComponentState._

  override def initialize(): Unit = {
    // DEOPSCSW-153: Accessibility of logging service to other CSW components
    log.info("Initializing Component TLA")
    Thread.sleep(100)

    // Publish the CurrentState using parameter set created using a sample Choice parameter
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))

    // DEOPSCSW-219: Discover component connection using HTTP protocol
    trackConnection(httpConnection)
    trackConnection(tcpConnection)
  }

  override def onGoOffline(): Unit =
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice))))

  override def onGoOnline(): Unit =
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice))))

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    // Adding passed in parameter to see if data is transferred properly
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(submitCommandChoice))))
    processCommand(controlCommand)
    Completed(runId)
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {
    // Adding passed in parameter to see if data is transferred properly
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(oneWayCommandChoice))))
    processCommand(controlCommand)
  }

  // DEOPSCSW-372: Provide an API for PubSubActor that hides actor based interaction
  private def processCommand(controlCommand: ControlCommand): Unit = {

    lazy val event = SystemEvent(Prefix("csw.move"), EventName("system"))
    def processEvent(prefix: Prefix): Event => Unit =
      _ =>
        currentStatePublisher.publish(
          CurrentState(prefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(eventReceivedChoice))
        )

    controlCommand match {
      case Setup(_, `setSeverityCommand`, _, _) => alarmService.setSeverity(testAlarmKey, testSeverity)

      case Setup(_, CommandName("publish.event.success"), _, _) => eventService.defaultPublisher.publish(event)

      case Setup(somePrefix, CommandName("subscribe.event.success"), _, _) =>
        eventService.defaultSubscriber.subscribeCallback(Set(event.eventKey), processEvent(somePrefix))

      case Setup(_, CommandName("time.service.scheduler.success"), _, _) =>
        timeServiceScheduler.scheduleOnce(UTCTime.now()) {
          currentStatePublisher.publish(CurrentState(prefix, timeServiceSchedulerState))
        }

      case Setup(somePrefix, _, _, _) =>
        currentStatePublisher.publish(
          CurrentState(somePrefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(setupConfigChoice))
        )

      case Observe(somePrefix, _, _, _) =>
        currentStatePublisher.publish(
          CurrentState(somePrefix, StateName("testStateName"), controlCommand.paramSet + choiceKey.set(observeConfigChoice))
        )
    }
  }

  def validateCommand(runId: Id, command: ControlCommand): ValidateCommandResponse = {
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice))))
    if (command.commandName.name.contains("success")) Accepted(runId)
    else Invalid(runId, OtherIssue("Testing: Received failure, will return Invalid."))
  }

  override def onShutdown(): Unit = {
    currentStatePublisher.publish(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
    Thread.sleep(500)
  }

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

  override def onOperationsMode(): Unit = {
    diagModeCancellable.foreach(_.cancel())
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit =
    trackingEvent match {
      case LocationUpdated(location) =>
        location.connection match {
          case _: AkkaConnection =>
            Future {
              Thread.sleep(500)
              currentStatePublisher.publish(
                CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationUpdatedChoice)))
              )
            }
          case _: HttpConnection =>
            currentStatePublisher.publish(
              CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(httpLocationUpdatedChoice)))
            )
          case _: TcpConnection =>
            currentStatePublisher.publish(
              CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(tcpLocationUpdatedChoice)))
            )
        }
      case LocationRemoved(connection) =>
        connection match {
          case _: AkkaConnection =>
            currentStatePublisher.publish(
              CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(akkaLocationRemovedChoice)))
            )
          case _: HttpConnection =>
            currentStatePublisher.publish(
              CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(httpLocationRemovedChoice)))
            )
          case _: TcpConnection =>
            currentStatePublisher.publish(
              CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(tcpLocationRemovedChoice)))
            )
        }
    }
}
