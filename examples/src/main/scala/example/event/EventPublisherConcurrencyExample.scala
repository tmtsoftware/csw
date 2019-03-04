package example.event
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.{ActorSystem, Cancellable, Scheduler}
import akka.util.Timeout
import csw.event.api.scaladsl.EventPublisher
import csw.params.core.generics.{Key, KeyType}
import csw.params.core.models.Prefix
import csw.params.events.{Event, EventName, SystemEvent}
import example.event.TemperatureMessage._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class EventPublisherConcurrencyExample(publisher: EventPublisher)(implicit actorSystem: ActorSystem) {

  private val temperatureActor: ActorRef[TemperatureMessage] = actorSystem.spawn(new TemperatureBehavior, "TemperatureActor")

  private implicit val timeout: Timeout     = Timeout(5.seconds)
  private implicit val scheduler: Scheduler = actorSystem.scheduler
  private implicit val ec: ExecutionContext = actorSystem.dispatcher

  private val prefix: Prefix                  = Prefix("wfos.blue.assembly")
  private val temperatureKey: Key[Int]        = KeyType.IntKey.make("temperature")
  private val temperatureEventName: EventName = EventName("temperature")

  // Callback where the mutation is required
  private def eventGenerator: Future[Some[Event]] = {
    val temperatureF = temperatureActor ? GetTemperature
    temperatureF.map(temp ⇒ {
      Some(SystemEvent(prefix, temperatureEventName).add(temperatureKey.set(temp.degrees)))
    })
  }

  // The Async API of Publisher is called since the eventGenerator returns a Future[Event]
  def publish(): Cancellable = publisher.publishAsync(eventGenerator, 50.millis)

}

// The Behavior of Actor which will handle the mutable state
class TemperatureBehavior extends AbstractBehavior[TemperatureMessage] {

  // mutable state which could be mutated from any methods from this class
  private var currentTemperature: Temperature = Temperature(0)

  override def onMessage(msg: TemperatureMessage): Behavior[TemperatureMessage] = {
    msg match {
      case GetTemperature(ref)   ⇒ ref ! currentTemperature
      case TemperatureRise(rise) ⇒ currentTemperature = Temperature(currentTemperature.degrees + rise)
      case TemperatureDrop(drop) ⇒ currentTemperature = Temperature(currentTemperature.degrees - drop)
    }
    this
  }
}

case class Temperature(degrees: Int)
trait TemperatureMessage
object TemperatureMessage {
  case class GetTemperature(ref: ActorRef[Temperature]) extends TemperatureMessage
  case class TemperatureRise(degrees: Int)              extends TemperatureMessage
  case class TemperatureDrop(degrees: Int)              extends TemperatureMessage
}
