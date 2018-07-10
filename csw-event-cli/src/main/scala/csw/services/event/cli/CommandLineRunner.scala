package csw.services.event.cli

import java.io.File
import java.time.Instant

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.KeyType.StructKey
import csw.messages.params.generics.Parameter
import csw.messages.params.models.{Id, Struct}
import csw.services.event.scaladsl.{EventService, EventSubscription}
import csw.services.event.scaladsl.SubscriptionModes.RateAdapterMode
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.async.Async.{async, await}
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CommandLineRunner(eventService: EventService, actorRuntime: ActorRuntime, printLine: Any ⇒ Unit) {

  import actorRuntime._

  def inspect(options: Options): Future[Unit] = async {
    val events    = await(getEvents(options.eventKeys))
    val formatter = OnelineFormatter(options)
    printLine(formatter.eventSeparator)

    events.foreach { event ⇒
      processOneline(event, options)
    }
  }

  private def makeCurrentPath(param: Parameter[_], parentKey: Option[String]) =
    if (parentKey.isDefined) s"${parentKey.get}/${param.keyName}"
    else param.keyName

  private def traverse(
      options: Options,
      parentKey: Option[String],
      params: Set[Parameter[_]],
      paths: List[String] = Nil
  ): List[Oneline] =
    params.toList.flatMap { param ⇒
      val currentPath = makeCurrentPath(param, parentKey)

      param.keyType match {
        case StructKey ⇒ traverse(options, Some(currentPath), param.values.flatMap(_.asInstanceOf[Struct].paramSet).toSet, paths)
        case _ if paths.isEmpty || paths.contains(currentPath) || paths.exists(p ⇒ currentPath.contains(p)) ⇒
          List(Oneline(currentPath, param))
        case _ ⇒ Nil
      }
    }

  private def processOneline(event: Event, options: Options, paths: List[String] = Nil): Unit = {
    val formatter = OnelineFormatter(options)

    printLine(formatter.header(event))

    if (isInvalid(event)) printLine(formatter.invalidKey(event.eventKey))
    val onelines = traverse(options, None, event.paramSet, paths)
    printLine(formatter.format(event, onelines))

    printLine(formatter.eventSeparator)
  }

  private def processGetJson(event: Event, paths: List[String], options: Options): Unit = {
    val formatter = JsonFormatter(options)
    if (isInvalid(event)) printLine(formatter.invalidKey(event.eventKey))

    val paths                = options.eventsMap(event.eventKey).toList
    val eventJson            = JsonSupport.writeEvent(event).as[JsObject]
    val transformedEventJson = EventJsonTransformer.transform(eventJson, paths)
    printLine(formatter.format(transformedEventJson))
  }

  def get(options: Options): Future[Unit] = async {
    val events = await(getEvents(options.eventsMap.keys.toSeq))

    if (options.isOneline) printLine(OnelineFormatter(options).eventSeparator)

    events.foreach { event ⇒
      processEvent(options, event)
    }
  }

  def publish(options: Options): Future[Done] = async {
    val event = readEventFromJson(options.eventData, options.eventKey)

    options.maybeInterval match {
      case Some(interval) ⇒ await(publishEventsWithInterval(event, interval, options.period))
      case None           ⇒ await(publishEvent(event))
    }
  }

  def subscribe(options: Options)(implicit ec: ExecutionContext, mat: Materializer): (Future[EventSubscription], Future[Done]) = {
    val keys        = options.eventsMap.keys.toSet
    val subscriberF = eventService.defaultSubscriber

    val eventStream = options.maybeInterval match {
      case Some(interval) => subscriberF.map(_.subscribe(keys, interval, RateAdapterMode))
      case None           => subscriberF.map(_.subscribe(keys))
    }

    if (options.isOneline) printLine(OnelineFormatter(options).eventSeparator)

    Source
      .fromFutureSource(eventStream)
      .toMat(Sink.foreach { event =>
        processEvent(options, event)
      })(Keep.both)
      .run()
  }

  private def processEvent(options: Options, event: Event): Unit = {
    val paths = options.eventsMap(event.eventKey).toList
    if (options.isOneline) processOneline(event, options, paths)
    if (options.isJson) processGetJson(event, paths, options)
  }

  private def isInvalid(event: Event): Boolean = event.eventTime == EventTime(Instant.ofEpochMilli(-1))

  private def getEvents(keys: Seq[EventKey]): Future[Seq[Event]] = async {
    val subscriber = await(eventService.defaultSubscriber)
    await(Future.traverse(keys)(subscriber.get))
  }

  private def readEventFromJson(data: File, maybeEventKey: Option[EventKey]) = {
    val eventJson = Json.parse(scala.io.Source.fromFile(data).mkString)
    JsonSupport.readEvent[Event](updateEventMetadata(eventJson, maybeEventKey))
  }

  private def updateEventMetadata(json: JsValue, maybeEventKey: Option[EventKey]) = {

    val updatedJson = json.as[JsObject] ++ Json.obj(
      ("eventId", Id().id),
      ("eventTime", EventTime().time)
    )

    maybeEventKey match {
      case Some(eventKey) =>
        updatedJson ++ Json.obj(
          ("source", eventKey.source.prefix),
          ("eventName", eventKey.eventName.name)
        )
      case None => updatedJson
    }
  }

  private def eventGenerator(initialEvent: Event) = initialEvent match {
    case e: SystemEvent  ⇒ e.copy(eventId = Id(), eventTime = EventTime())
    case e: ObserveEvent ⇒ e.copy(eventId = Id(), eventTime = EventTime())
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

  private def publishEventsWithInterval(initialEvent: Event, interval: FiniteDuration, duration: FiniteDuration) =
    Source
      .tick(0.millis, interval, ())
      .map(_ ⇒ eventGenerator(initialEvent))
      .takeWithin(duration)
      .map(publishEvent)
      .toMat(Sink.ignore)(Keep.right)
      .run()
}
