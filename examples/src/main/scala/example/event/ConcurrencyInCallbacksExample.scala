package example.event
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{ActorSystem, Scheduler}
import akka.util.Timeout
import csw.event.api.scaladsl.EventPublisher
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventName, SystemEvent}
import example.event.TemperatureMessage._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class ConcurrencyInCallbacksExample(publisher: EventPublisher)(implicit actorSystem: ActorSystem) {

  // currentTemperature is the mutable state which needs to be mutated from anywhere from the program
  def behavior(currentTemperature: Temperature): Behavior[TemperatureMessage] = Behaviors.setup { ctx ⇒
    implicit val timeout: Timeout     = Timeout(5.seconds)
    implicit val scheduler: Scheduler = actorSystem.scheduler
    implicit val ec: ExecutionContext = actorSystem.dispatcher

    val prefix: Prefix                  = Prefix("wfos.blue.assembly")
    val temperatureKey: Key[Int]        = KeyType.IntKey.make("temperature")
    val temperatureEventName: EventName = EventName("temperature")

    // Callback where the mutation is required
    def eventGenerator: Future[Some[Event]] = {
      val temperatureF = ctx.self ? GetTemperature
      temperatureF.map(temp ⇒ {
        Some(SystemEvent(prefix, temperatureEventName).add(temperatureKey.set(temp.degrees)))
      })
    }

    Behaviors.receiveMessage {
      case GetTemperature(ref) ⇒ ref ! currentTemperature; Behaviors.same

      case PublishTemperature ⇒
        // The Async API of Publisher is called since the eventGenerator returns a Future[Event]
        publisher.publishAsync(eventGenerator, 50.millis)
        Behaviors.same

      case TemperatureRise(rise) ⇒ behavior(Temperature(currentTemperature.degrees + rise))
      case TemperatureDrop(drop) ⇒ behavior(Temperature(currentTemperature.degrees - drop))

    }
  }

}

case class Temperature(degrees: Int)
trait TemperatureMessage
object TemperatureMessage {
  case class GetTemperature(ref: ActorRef[Temperature]) extends TemperatureMessage
  case object PublishTemperature                        extends TemperatureMessage
  case class TemperatureRise(degrees: Int)              extends TemperatureMessage
  case class TemperatureDrop(degrees: Int)              extends TemperatureMessage
}
