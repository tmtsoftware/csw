/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.event.cli

import java.io.File

import org.apache.pekko.Done
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.stream.scaladsl.{Keep, Sink, Source}
import org.apache.pekko.stream.{KillSwitches}
import csw.event.api.scaladsl.SubscriptionModes.RateAdapterMode
import csw.event.api.scaladsl.{EventService, EventSubscription}
import csw.event.cli.args.Options
import csw.event.cli.extenstion.RichStringExtentions.JsonDecodeRichString
import csw.event.cli.utils.{EventOnelineTransformer, EventTransformer, Formatter}
import csw.event.cli.wiring.ActorRuntime
import csw.params.core.formats.JsonSupport
import csw.params.core.generics.Parameter
import csw.params.core.models.Id
import csw.params.events.*
import csw.time.core.models.UTCTime
import play.api.libs.json.Json

import cps.compat.FutureAsync.*
import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.{Failure, Success}

class CommandLineRunner(eventService: EventService, actorRuntime: ActorRuntime, printLine: Any => Unit) {

  import actorRuntime._

  def inspect(options: Options): Future[Unit] =
    async {
      val events = await(getEvents(options.eventKeys))
      new EventOnelineTransformer(options).transform(events).foreach(printLine)
    }

  def get(options: Options): Future[Unit] =
    async {
      val events = await(getEvents(options.eventsMap.keys.toSeq))
      if (options.isJsonOut)
        events.foreach(event => processGetJson(event, options))
      else new EventOnelineTransformer(options).transform(events).foreach(printLine)
    }

  def publish(options: Options): Future[Done] =
    async {
      val event        = await(getEvent(options.eventKey, options.eventData))
      val updatedEvent = updateEventParams(event, options.params)

      options.maybeInterval match {
        case Some(interval) => await(publishEventsWithInterval(updatedEvent, interval, options.period))
        case None           => await(publishEvent(updatedEvent))
      }
    }

  def subscribe(options: Options): (EventSubscription, Future[Done]) = {
    val keys        = options.eventsMap.keys.toSet
    val subscriberF = eventService.defaultSubscriber

    val eventStream = options.maybeInterval match {
      case Some(interval) => subscriberF.subscribe(keys, interval, RateAdapterMode)
      case None           => subscriberF.subscribe(keys)
    }

    if (options.isOnelineOut) printLine(Formatter.EventSeparator)

    val (subscriptionF, doneF) = eventStream
      .toMat(Sink.foreach(processEvent(options, _)))(Keep.both)
      .run()

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "unsubscribe-stream"
    )(() => subscriptionF.unsubscribe())

    (subscriptionF, doneF)
  }

  private def processEvent(options: Options, event: Event): Unit =
    if (options.isJsonOut) processGetJson(event, options)
    else new EventOnelineTransformer(options).transform(event).foreach(printLine)

  private def processGetJson(event: Event, options: Options): Unit = {
    if (event.isInvalid) printLine(Formatter.invalidKey(event.eventKey))
    else {
      val paths                = options.paths(event.eventKey)
      val transformedEventJson = EventTransformer.transform(event, paths)
      printLine(Json.prettyPrint(JsonSupport.writeEvent(transformedEventJson)))
    }
  }

  private def getEvents(keys: Seq[EventKey]) = {
    val subscriber = eventService.defaultSubscriber
    Future.traverse(keys)(subscriber.get)
  }

  private def getEvent(key: EventKey, eventData: Option[File]) = {
    val subscriber = eventService.defaultSubscriber
    eventData match {
      case Some(file) => Future.successful(readEventFromJson(file, key))
      case None       => subscriber.get(key)
    }
  }

  import csw.params.core.formats.ParamCodecs._
  private def readEventFromJson(data: File, eventKey: EventKey) = {
    val event = scala.io.Source.fromFile(data).mkString.parse[Event]
    event match {
      case se @ SystemEvent(_, _, _, _, paramSet)  => se.copy(Id(), eventKey.source, eventKey.eventName, UTCTime.now(), paramSet)
      case oe @ ObserveEvent(_, _, _, _, paramSet) => oe.copy(Id(), eventKey.source, eventKey.eventName, UTCTime.now(), paramSet)
    }
  }

  private def updateEventParams(event: Event, paramSet: Set[Parameter[_]]) =
    event match {
      case event: SystemEvent  => event.madd(paramSet)
      case event: ObserveEvent => event.madd(paramSet)
    }

  private def eventGenerator(initialEvent: Event): Event =
    initialEvent match {
      case event: SystemEvent  => event.copy(eventId = Id(), eventTime = UTCTime.now())
      case event: ObserveEvent => event.copy(eventId = Id(), eventTime = UTCTime.now())
    }

  private def publishEvent(event: Event): Future[Done] = {
    val publisher     = eventService.defaultPublisher
    val publishResult = publisher.publish(event)
    publishResult.onComplete {
      case Success(_) => printLine(s"[SUCCESS] Event [${event.eventKey}] published successfully")
      case Failure(ex) =>
        printLine(s"[FAILURE] Failed to publish event [${event.eventKey}] with error: [${ex.getCause.getMessage}]")
    }
    publishResult
  }

  private def publishEventsWithInterval(initialEvent: Event, interval: FiniteDuration, duration: FiniteDuration) = {
    val (killSwitch, doneF) = Source
      .tick(0.millis, interval, ())
      .viaMat(KillSwitches.single)(Keep.right)
      .map(_ => eventGenerator(initialEvent))
      .takeWithin(duration)
      .map(publishEvent)
      .toMat(Sink.ignore)(Keep.both)
      .run()

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "shutdown-publish-stream"
    )(() => Future { killSwitch.shutdown(); Done })

    doneF
  }

}
