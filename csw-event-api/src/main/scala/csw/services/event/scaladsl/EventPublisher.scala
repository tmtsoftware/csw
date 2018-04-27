package csw.services.event.scaladsl

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import csw.messages.events.Event
import csw.services.event.exceptions.PublishFailed
import csw.services.event.javadsl.IEventPublisher

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait EventPublisher {

  def publish(event: Event): Future[Done]

  def publish[Mat](source: Source[Event, Mat]): Mat

  def publish[Mat](source: Source[Event, Mat], onError: (Event, PublishFailed) ⇒ Unit): Mat

  def publish(eventGenerator: => Event, every: FiniteDuration): Cancellable

  def publish(eventGenerator: ⇒ Event, every: FiniteDuration, onError: (Event, PublishFailed) ⇒ Unit): Cancellable

  def shutdown(): Future[Done]

  def asJava: IEventPublisher
}
