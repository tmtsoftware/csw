package csw.services.event.scaladsl

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.Source
import csw.messages.ccs.events.Event

import scala.concurrent.Future
import scala.concurrent.duration.{DurationDouble, FiniteDuration}

trait EventPublisher {
  def publish[Mat](source: Source[Event, Mat]): Mat
  def publish(event: Event): Future[Done]

  def publish(eventGenerator: () => Event, every: FiniteDuration): Cancellable = {
    val stream = Source.tick(0.millis, every, ()).map(_ => eventGenerator())
    publish(stream)
  }

  def shutdown(): Future[Done]
}
