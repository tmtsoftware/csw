package csw.services.event.cli

import java.io.File

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.stream.{KillSwitches, Materializer}
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.Parameter
import csw.messages.params.models.Id
import csw.services.event.cli.args.Options
import csw.services.event.cli.utils.{EventJsonTransformer, EventOnelineTransformer, Formatter}
import csw.services.event.cli.wiring.ActorRuntime
import csw.services.event.api.scaladsl.SubscriptionModes.RateAdapterMode
import csw.services.event.api.scaladsl.{EventService, EventSubscription}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.async.Async.{async, await}
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CommandLineRunner(eventService: EventService, actorRuntime: ActorRuntime, printLine: Any ⇒ Unit) {

  import actorRuntime._

  def inspect(options: Options): Future[Unit] = async {
    val events = await(getEvents(options.eventKeys))
    new EventOnelineTransformer(options).transform(events).foreach(printLine)
  }

  def get(options: Options): Future[Unit] = async {
    val events = await(getEvents(options.eventsMap.keys.toSeq))
    if (options.isOneline) new EventOnelineTransformer(options).transform(events).foreach(printLine)
    else events.foreach(event ⇒ processGetJson(event, options))
  }

  def publish(options: Options): Future[Done] = async {
    val event        = await(getEvent(options.eventKey, options.eventData))
    val updatedEvent = updateEventParams(event, options.params)

    options.maybeInterval match {
      case Some(interval) ⇒ await(publishEventsWithInterval(updatedEvent, interval, options.period))
      case None           ⇒ await(publishEvent(updatedEvent))
    }
  }

  def subscribe(options: Options)(implicit ec: ExecutionContext, mat: Materializer): (Future[EventSubscription], Future[Done]) = {
    val keys        = options.eventsMap.keys.toSet
    val subscriberF = eventService.defaultSubscriber

    val eventStream = options.maybeInterval match {
      case Some(interval) => subscriberF.map(_.subscribe(keys, interval, RateAdapterMode))
      case None           => subscriberF.map(_.subscribe(keys))
    }

    if (options.isOneline) printLine(Formatter.eventSeparator)

    val (subscriptionF, doneF) = Source
      .fromFutureSource(eventStream)
      .toMat(Sink.foreach { event =>
        processEvent(options, event)
      })(Keep.both)
      .run()

    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "unsubscribe-stream"
    )(() ⇒ subscriptionF.flatMap(_.unsubscribe()))

    (subscriptionF, doneF)
  }

  private def processEvent(options: Options, event: Event): Unit =
    if (options.isOneline) new EventOnelineTransformer(options).transform(event).foreach(printLine)
    else processGetJson(event, options)

  private def processGetJson(event: Event, options: Options): Unit = {
    if (event.isInvalid) printLine(Formatter.invalidKey(event.eventKey))
    else {
      val paths                = options.paths(event.eventKey)
      val eventJson            = JsonSupport.writeEvent(event).as[JsObject]
      val transformedEventJson = EventJsonTransformer.transform(eventJson, paths)
      printLine(Json.prettyPrint(transformedEventJson))
    }
  }

  private def getEvents(keys: Seq[EventKey]) = async {
    val subscriber = await(eventService.defaultSubscriber)
    await(Future.traverse(keys)(subscriber.get))
  }

  private def getEvent(key: EventKey, eventData: Option[File]) = async {
    val subscriber = await(eventService.defaultSubscriber)
    eventData match {
      case Some(file) ⇒ readEventFromJson(file, key)
      case None       ⇒ await(subscriber.get(key))
    }
  }

  private def readEventFromJson(data: File, eventKey: EventKey) = {
    val eventJson = Json.parse(scala.io.Source.fromFile(data).mkString)
    JsonSupport.readEvent[Event](updateEventMetadata(eventJson, eventKey))
  }

  private def updateEventMetadata(json: JsValue, eventKey: EventKey) =
    json.as[JsObject] ++ Json.obj(
      ("eventId", Id().id),
      ("eventTime", EventTime().time),
      ("source", eventKey.source.prefix),
      ("eventName", eventKey.eventName.name)
    )

  private def updateEventParams(event: Event, paramSet: Set[Parameter[_]]) = event match {
    case event: SystemEvent  ⇒ event.madd(paramSet)
    case event: ObserveEvent ⇒ event.madd(paramSet)
  }

  private def eventGenerator(initialEvent: Event): Event = initialEvent match {
    case event: SystemEvent  ⇒ event.copy(eventId = Id(), eventTime = EventTime())
    case event: ObserveEvent ⇒ event.copy(eventId = Id(), eventTime = EventTime())
  }

  private def publishEvent(event: Event) = async {
    val publisher     = await(eventService.defaultPublisher)
    val publishResult = publisher.publish(event)
    publishResult.onComplete {
      case Success(_) ⇒ printLine(s"[SUCCESS] Event [${event.eventKey}] published successfully")
      case Failure(ex) ⇒
        printLine(s"[FAILURE] Failed to publish event [${event.eventKey}] with error: [${ex.getCause.getMessage}]")
    }
    await(publishResult)
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
