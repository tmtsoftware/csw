package csw.services.event.cli

import java.time.Instant

import csw.messages.events.{Event, EventKey, EventTime}
import csw.messages.params.formats.JsonSupport
import csw.messages.params.generics.KeyType.StructKey
import csw.messages.params.generics.Parameter
import csw.messages.params.models.Struct
import csw.services.event.scaladsl.EventService
import play.api.libs.json.Json

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
    val events = await(getEvents(options.eventKeys))

    prettyPrintEventsJson(events)
  }

  private def getEvents(keys: Seq[EventKey]): Future[Seq[Event]] = async {
    val subscriber = await(eventService.defaultSubscriber)
    await(Future.traverse(keys)(subscriber.get))
  }

  private def prettyPrintEventsJson(events: Seq[Event]): Unit =
    events.foreach(event ⇒ printLine(Json.prettyPrint(JsonSupport.writeEvent(event))))

  private def isInvalid(event: Event): Boolean = event.eventTime == EventTime(Instant.ofEpochMilli(-1))
}
