package csw.event.cli

import java.io.File

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer}
import csw.event.api.scaladsl.SubscriptionModes.RateAdapterMode
import csw.event.api.scaladsl.{EventService, EventSubscription}
import csw.event.cli.args.Options
import csw.event.cli.utils.{EventJsonTransformer, EventOnelineTransformer, Formatter}
import csw.event.cli.wiring.ActorRuntime
import csw.params.core.formats.JsonSupport
import csw.params.core.generics.Parameter
import csw.params.core.models.Id
import csw.params.events._
import csw.time.core.models.UTCTime
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.util.{Failure, Success}

class CommandLineRunner(eventService: EventService, actorRuntime: ActorRuntime, printLine: Any ⇒ Unit) {

  import actorRuntime._

  def inspect(options: Options): Future[Unit] = async {
    val events = await(getEvents(options.eventKeys))
    new EventOnelineTransformer(options).transform(events).foreach(printLine)
  }

  def get(options: Options): Future[Unit] = async {
    val events = await(getEvents(options.eventsMap.keys.toSeq))
    if (options.isJsonOut)
      events.foreach(event ⇒ processGetJson(event, options))
    else new EventOnelineTransformer(options).transform(events).foreach(printLine)
  }

  def publish(options: Options): Future[Done] = async {
    val event        = await(getEvent(options.eventKey, options.eventData))
    val updatedEvent = updateEventParams(event, options.params)

    options.maybeInterval match {
      case Some(interval) ⇒ await(publishEventsWithInterval(updatedEvent, interval, options.period))
      case None           ⇒ await(publishEvent(updatedEvent))
    }
  }

  def subscribe(options: Options)(implicit mat: Materializer): (EventSubscription, Future[Done]) = {
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
    )(() ⇒ subscriptionF.unsubscribe())

    (subscriptionF, doneF)
  }

  private def processEvent(options: Options, event: Event): Unit =
    if (options.isJsonOut) processGetJson(event, options)
    else new EventOnelineTransformer(options).transform(event).foreach(printLine)

  private def processGetJson(event: Event, options: Options): Unit = {
    if (event.isInvalid) printLine(Formatter.invalidKey(event.eventKey))
    else {
      val paths                = options.paths(event.eventKey)
      val eventJson            = JsonSupport.writeEvent(event).as[JsObject]
      val transformedEventJson = EventJsonTransformer.transform(eventJson, paths)
      printLine(Json.prettyPrint(transformedEventJson))
    }
  }

  private def getEvents(keys: Seq[EventKey]) = {
    val subscriber = eventService.defaultSubscriber
    Future.traverse(keys)(subscriber.get)
  }

  private def getEvent(key: EventKey, eventData: Option[File]) = {
    val subscriber = eventService.defaultSubscriber
    eventData match {
      case Some(file) ⇒ Future.successful(readEventFromJson(file, key))
      case None       ⇒ subscriber.get(key)
    }
  }

  private def readEventFromJson(data: File, eventKey: EventKey) = {
    val eventJson = Json.parse(scala.io.Source.fromFile(data).mkString)
    JsonSupport.readEvent[Event](updateEventMetadata(eventJson, eventKey))
  }

  private def updateEventMetadata(json: JsValue, eventKey: EventKey) =
    json.as[JsObject] ++ Json.obj(
      ("eventId", Id().id),
      ("eventTime", UTCTime.now()),
      ("source", eventKey.source.prefix),
      ("eventName", eventKey.eventName.name)
    )

  private def updateEventParams(event: Event, paramSet: Set[Parameter[_]]) = event match {
    case event: SystemEvent  ⇒ event.madd(paramSet)
    case event: ObserveEvent ⇒ event.madd(paramSet)
  }

  private def eventGenerator(initialEvent: Event): Event = initialEvent match {
    case event: SystemEvent  ⇒ event.copy(eventId = Id(), eventTime = UTCTime.now())
    case event: ObserveEvent ⇒ event.copy(eventId = Id(), eventTime = UTCTime.now())
  }

  private def publishEvent(event: Event): Future[Done] = {
    val publisher     = eventService.defaultPublisher
    val publishResult = publisher.publish(event)
    publishResult.onComplete {
      case Success(_) ⇒ printLine(s"[SUCCESS] Event [${event.eventKey}] published successfully")
      case Failure(ex) ⇒
        printLine(s"[FAILURE] Failed to publish event [${event.eventKey}] with error: [${ex.getCause.getMessage}]")
    }
    publishResult
  }

  private def publishEventsWithInterval(initialEvent: Event, interval: FiniteDuration, duration: FiniteDuration) = {
    val (killSwitch, doneF) = Source
      .tick(0.millis, interval, ())
      .viaMat(KillSwitches.single)(Keep.right)
      .map(_ ⇒ eventGenerator(initialEvent))
      .takeWithin(duration)
      .map(publishEvent)
      .toMat(Sink.ignore)(Keep.both)
      .run()

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "shutdown-publish-stream"
    )(() ⇒ Future { killSwitch.shutdown(); Done })

    doneF
  }

}
