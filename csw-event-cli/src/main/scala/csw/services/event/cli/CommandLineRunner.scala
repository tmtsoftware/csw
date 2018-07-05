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

  private val footer = "========================================================================="

  def inspect(options: Options): Future[Unit] = async {

    val events = await(getEvents(options.eventKeys))
    events.foreach { event ⇒
      printHeader(event, options)
      if (isInvalid(event)) printForInvalidKey(event.eventKey)
      else traverse(options, None, event.paramSet).sorted.foreach(printLine)
      printLine(footer)
    }
  }

  private def printForInvalidKey(eventKey: EventKey): Unit =
    printLine(s"$eventKey [ERROR] No events published for this key.")

  private def makeCurrentPath(param: Parameter[_], parentKey: Option[String]) =
    if (parentKey.isDefined) s"${parentKey.get}/${param.keyName}"
    else param.keyName

  private def traverse(
      options: Options,
      parentKey: Option[String],
      params: Set[Parameter[_]],
      paths: List[String] = Nil
  ): List[String] =
    params.flatMap { param ⇒
      val currentPath = makeCurrentPath(param, parentKey)

      val maybePathInfo = {
        if (paths.isEmpty || paths.contains(currentPath)) {
          //|| paths.exists(path => path.startsWith(s"$currentPath/")))) {
          Some(formatOneline(options, param, currentPath))
        } else None
      }

      val innerPathInfos = param.keyType match {
        case StructKey ⇒ traverse(options, Some(currentPath), param.values.flatMap(_.asInstanceOf[Struct].paramSet).toSet, paths)
        case _         ⇒ Nil
      }

      maybePathInfo match {
        case Some(pathInfo) ⇒ pathInfo :: innerPathInfos
        case None           ⇒ innerPathInfos
      }

    }.toList

  private def formatOneline(options: Options, param: Parameter[_], currentPath: String) = {
    if (options.cmd == "get") {
      var values = s"$currentPath = ${param.values.mkString("[", ",", "]")}"
      if (options.printUnits) values += s" [${param.units}]"
      values
    } else s"$currentPath = ${param.keyType}[${param.units}]"

  }

  def get(options: Options): Future[Unit] = async {
    val transformer = new EventJsonTransformer(printLine, options)
    val events      = await(getEvents(options.eventsMap.keys.toSeq))

    events.foreach { event ⇒
      val paths = options.eventsMap(event.eventKey).toList
      event match {
        case _ if isInvalid(event) ⇒ printForInvalidKey(event.eventKey)

        case _ if options.isOneline ⇒
          printHeader(event, options)
          traverse(options, None, event.paramSet, paths).sorted.foreach(printLine)
          printLine(footer)
          printLine("")

        case _ if options.isJson ⇒
          val eventJson = PlayJson.transform(JsonSupport.writeEvent(event), upickle.default.reader[Js.Obj])
          transformer.transformInPlace(eventJson, paths)
          printLine(write(eventJson, 4))
      }
    }
  }

  private def printHeader(event: Event, options: Options): Unit = {
    val timestamp = if (options.printTimestamp) event.eventTime.time.toString else ""
    val id        = if (options.printId) event.eventId.id else ""
    val header    = List(timestamp, id, event.eventKey.key).filter(_.nonEmpty).mkString(" ")
    printLine(header)
    printLine("")
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
