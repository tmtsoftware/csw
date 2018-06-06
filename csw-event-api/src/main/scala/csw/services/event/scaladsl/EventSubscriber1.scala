package csw.services.event.scaladsl

import akka.actor.typed.ActorRef
import akka.stream.scaladsl.Source
import csw.messages.events.{Event, EventKey}

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, DurationDouble}
import scala.util.matching.Regex

class EventSubscriber1 {
  def subscribe(keys: Keys, throttleMode: ThrottleMode): Source[Event, EventSubscription] = ???

  def subscribe(keys: Keys, action: Action, throttleMode: ThrottleMode): EventSubscription = ???
}

sealed trait Keys
object Keys {
  case class EventKeys(keys: Set[EventKey]) extends Keys
  case class Pattern(pattern: Regex)        extends Keys
}

sealed trait Action
object Action {
  case class Callback(f: Event => Unit)              extends Action
  case class ToActorRef(ref: ActorRef[Event])        extends Action
  case class AsyncCallback(f: Event => Future[Unit]) extends Action
}

sealed trait ThrottleMode
object ThrottleMode {
  case object None                    extends ThrottleMode
  case class Limiter(every: Duration) extends ThrottleMode
  case class Adapter(every: Duration) extends ThrottleMode
}

object Demo {
  private val eventSubscriber = new EventSubscriber1()

  private val keys = Set(EventKey("test.prefix.system"))

  eventSubscriber.subscribe(Keys.EventKeys(keys), ThrottleMode.Adapter(10.millis))
  eventSubscriber.subscribe(Keys.Pattern("test.*".r), ThrottleMode.Adapter(10.millis))

  eventSubscriber.subscribe(Keys.EventKeys(keys), Action.Callback(println), ThrottleMode.Adapter(10.millis))

  eventSubscriber.subscribe(Keys.EventKeys(keys), Action.Callback(println), ThrottleMode.None)
}
