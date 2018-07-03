package csw.services.event.cli

import java.time.Instant

import akka.japi.Option.Some
import csw.messages.events.{Event, EventKey, EventTime}
import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.KeyType.StructKey
import csw.messages.params.generics.Parameter
import csw.messages.params.models.Struct
import csw.services.event.scaladsl.EventService
import ujson.Js
import ujson.play.PlayJson
import upickle.default.write

import scala.async.Async.{async, await}
import scala.concurrent.Future

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
}
