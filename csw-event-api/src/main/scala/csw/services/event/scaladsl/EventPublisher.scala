package csw.services.event.scaladsl

import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import csw.messages.ccs.events.Event

import scala.concurrent.Future

trait EventPublisher {
  def publish(source: Source[Event, NotUsed]): Future[Done]
  def publish(event: Event): Future[Done]
}
