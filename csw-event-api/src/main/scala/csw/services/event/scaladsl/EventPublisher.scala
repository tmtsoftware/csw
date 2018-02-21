package csw.services.event.scaladsl

import akka.stream.OverflowStrategy
import akka.{Done, NotUsed}
import akka.stream.scaladsl.{Source, SourceQueueWithComplete}
import csw.messages.ccs.events.Event

import scala.concurrent.Future

trait EventPublisher {
  def publish(source: Source[Event, NotUsed]): Future[Done]
  def publish(event: Event): Future[Done]
  def queue(bufferSize: Int = 1024,
            overflowStrategy: OverflowStrategy = OverflowStrategy.backpressure): SourceQueueWithComplete[Event]
}
