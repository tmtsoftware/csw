package csw.services.event.scaladsl

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailed

import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

trait EventPublisher {

  def publish(event: Event): Future[Done]

  def publish[Mat](source: Source[Event, Mat]): Mat

  def publish[Mat](source: Source[Event, Mat], onError: (Event, PublishFailed) ⇒ Unit): Mat

  def publish(eventGenerator: => Event, every: FiniteDuration): Cancellable = publish(eventStream(eventGenerator, every))

  def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: (Event, PublishFailed) ⇒ Unit): Cancellable =
    publish(eventStream(eventGenerator, every), onError)

  def shutdown(): Future[Done]

  private def eventStream(eventGenerator: => Event, every: FiniteDuration): Source[Event, Cancellable] =
    Source.tick(0.millis, every, ()).map(_ => eventGenerator)

}
