package csw.services.event.internal.pubsub

import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import csw.messages.events.Event
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.duration.{DurationDouble, FiniteDuration}

abstract class AbstractEventPublisher extends EventPublisher {

  override def publish(eventGenerator: => Event, every: FiniteDuration): Cancellable = publish(eventStream(eventGenerator, every))

  override def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: Event ⇒ Unit): Cancellable =
    publish(eventStream(eventGenerator, every), onError)

  private def eventStream(eventGenerator: => Event, every: FiniteDuration): Source[Event, Cancellable] =
    Source.tick(0.millis, every, ()).map(_ => eventGenerator)
}
