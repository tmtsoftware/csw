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
import play.api.libs.json.Json
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

    def inspect0(eventKey: String, parentKey: Option[String], params: Set[Parameter[_]]): Unit = {
      def keyName(param: Parameter[_]) =
        if (parentKey.isDefined) s"${parentKey.get}/${param.keyName}"
        else param.keyName

      params.foreach { param ⇒
        param.keyType match {
          case StructKey ⇒
            printLine(s"$eventKey ${keyName(param)} = ${param.keyType}[${param.units}]")
            inspect0(eventKey, Some(keyName(param)), param.values.flatMap(_.asInstanceOf[Struct].paramSet).toSet)
          case _ ⇒
            printLine(s"$eventKey ${keyName(param)} = ${param.keyType}[${param.units}]")
        }
      }
    }

    val events = await(getEvents(options.eventKeys))
    events.foreach { event ⇒
      val eventKey = s"${event.eventKey.source.prefix}.${event.eventKey.eventName}"
      val params   = event.paramSet
      if (isInvalid(event)) printLine(s"$eventKey [ERROR] No events published for this key.")
      else inspect0(eventKey, None, params)
      printLine("=========================================================================")
    }
  }

  def get(options: Options): Future[Unit] = async {
    val events = await(getEvents(options.eventsMap.keys.toSeq))

    events.foreach(e ⇒ {
      val eventJson = PlayJson.transform(JsonSupport.writeEvent(e), upickle.default.reader[Js.Obj])
      val paths     = options.eventsMap(e.eventKey).map(_.split("/").toList).toList
      transformInPlace(eventJson, None, buildIncrementalPath(paths))
      printLine(write(eventJson, 4))
    })
  }

  def publish(options: Options): Future[Done] = async {
    val event = readEventFromJson(options.eventData)

    options.interval match {
      case Some(interval) ⇒ await(publishEventsWithInterval(event, interval, options.duration))
      case None           ⇒ await(publishEvent(event))
    }
  }

  private def isInvalid(event: Event): Boolean = event.eventTime == EventTime(Instant.ofEpochMilli(-1))

  private def getEvents(keys: Seq[EventKey]): Future[Seq[Event]] = async {
    val subscriber = await(eventService.defaultSubscriber)
    await(Future.traverse(keys)(subscriber.get))
  }

  private def buildIncrementalPath(paths: List[List[String]]) = paths.map { path ⇒
    var last = ""
    path.map { segment ⇒
      last = if (last.nonEmpty) last + "/" + segment else segment
      last
    }
  }

  private def breakIncrementalPaths(allIncrementalPaths: List[List[String]]) = {
    allIncrementalPaths.map {
      case Nil                                            ⇒ ("", Nil)
      case currentIncrementalPath :: nextIncrementalPaths ⇒ (currentIncrementalPath, nextIncrementalPaths)
    }.unzip
  }

  private def transformInPlace(json: Js.Obj, parentPath: Option[String], allIncrementalPaths: List[List[String]]): Unit =
    allIncrementalPaths match {
      case Nil ⇒
      case _ ⇒
        val (allCurrentIncrementalPaths, allNextIncrementalPaths) = breakIncrementalPaths(allIncrementalPaths)

        allCurrentIncrementalPaths.filter(_.nonEmpty) match {
          case Nil =>
          case _ =>
            def currentPath(json: Js.Value): String = {
              val keyName = json("keyName").str
              if (parentPath.isDefined) s"${parentPath.get}/$keyName"
              else keyName
            }

            json("paramSet") = json("paramSet").arr.filter(param ⇒ allCurrentIncrementalPaths.contains(currentPath(param)))

            json("paramSet").arr.foreach { param =>
              param("values") = param("values").arr.filter {
                case value: Js.Obj =>
                  transformInPlace(value, Some(currentPath(param)), allNextIncrementalPaths)
                  value("paramSet").arr.nonEmpty //Remove empty paramSets
                case _ => true
              }
            }
        }
    }

  private def readEventFromJson(data: File) = {
    val eventJson = Json.parse(scala.io.Source.fromFile(data).mkString)
    JsonSupport.readEvent[Event](eventJson)
  }

  private def eventGenerator(initialEvent: Event) = initialEvent match {
    case e: SystemEvent  ⇒ e.copy(eventId = Id(), eventTime = EventTime())
    case e: ObserveEvent ⇒ e.copy(eventId = Id(), eventTime = EventTime())
  }

  private def publishEvent(event: Event) = async {
    val publisher     = await(eventService.defaultPublisher)
    val publishResult = publisher.publish(event)
    publishResult.onComplete {
      case Success(_)  ⇒ printLine(s"Event [${event.eventKey}] published successfully")
      case Failure(ex) ⇒ printLine(s"Failed to publish event [${event.eventKey}] with cause: [${ex.getCause.getMessage}]")
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
