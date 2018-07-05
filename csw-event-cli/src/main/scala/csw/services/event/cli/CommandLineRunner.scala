package csw.services.event.cli

import java.io.File
import java.time.Instant

import akka.Done
import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.events._
import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.KeyType.StructKey
import csw.messages.params.generics.Parameter
import csw.messages.params.models.{Id, Struct}
import csw.services.event.scaladsl.EventService
import play.api.libs.json.{JsObject, JsValue, Json}
import ujson.Js
import ujson.play.PlayJson
import upickle.default.write

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.util.{Failure, Success}

class CommandLineRunner(eventService: EventService, actorRuntime: ActorRuntime, printLine: Any ⇒ Unit) {

  import actorRuntime._

  def inspect(options: Options): Future[Unit] = async {

    val events = await(getEvents(options.eventKeys))
    events.foreach { event ⇒
      val eventKey = s"${event.eventKey.source.prefix}.${event.eventKey.eventName}"
      val params   = event.paramSet
      if (isInvalid(event)) printLine(s"$eventKey [ERROR] No events published for this key.")
      else traverse(eventKey, None, params).foreach(printLine)
      printLine("=========================================================================")
    }
  }

  private def makeCurrentPath(param: Parameter[_], parentKey: Option[String]) =
    if (parentKey.isDefined) s"${parentKey.get}/${param.keyName}"
    else param.keyName

  private def traverse(eventKey: String, parentKey: Option[String], params: Set[Parameter[_]]): List[String] =
    params.flatMap { param ⇒
      val currentPath = makeCurrentPath(param, parentKey)
      val pathInfo    = s"$eventKey $currentPath = ${param.keyType}[${param.units}]"

      val innerPathInfos = param.keyType match {
        case StructKey ⇒ traverse(eventKey, Some(currentPath), param.values.flatMap(_.asInstanceOf[Struct].paramSet).toSet)
        case _         ⇒ Nil
      }
      pathInfo :: innerPathInfos
    }.toList

  def get(options: Options): Future[Unit] = async {
    val transformer = new EventJsonTransformer(printLine, options)

    val events = await(getEvents(options.eventsMap.keys.toSeq))
    events.foreach(e ⇒ {
      val eventJson = PlayJson.transform(JsonSupport.writeEvent(e), upickle.default.reader[Js.Obj])
      val paths     = options.eventsMap(e.eventKey).toList
      if (options.isOneline) printHeader(e.eventKey, eventJson, options)
      transformer.transformInPlace(eventJson, paths)
      if (options.isJson) printLine(write(eventJson, 4))

    })
  }

  private def printHeader(eventKey: EventKey, eventJson: Js.Obj, options: Options): Unit = {
    val timestamp = if (options.printTimestamp) eventJson("eventTime").str else ""
    val id        = if (options.printId) eventJson("eventId").str else ""
    val header    = List(timestamp, id, eventKey.key).filter(_.nonEmpty).mkString(" ")
    printLine(header)
  }

  def publish(options: Options): Future[Done] = async {
    val event = readEventFromJson(options.eventData, options.eventKey)

    options.interval match {
      case Some(interval) ⇒ await(publishEventsWithInterval(event, interval, options.period))
      case None           ⇒ await(publishEvent(event))
    }
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
