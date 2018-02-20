package csw.services.event.internal.pubsub

import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import csw.messages.ccs.events.Event
import csw.services.event.internal.api.EventPublishDriver
import csw.services.event.scaladsl.EventPublisher

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

class EventPublisherImpl(eventPublishDriver: EventPublishDriver)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventPublisher {

  override def publish(source: Source[Event, NotUsed]): Future[Done] = source.mapAsync(1)(publish).runWith(Sink.ignore)

  override def publish(event: Event): Future[Done] = async {
    await(eventPublishDriver.publish(event.eventKey, event))
    await(eventPublishDriver.set(event.eventKey, event))
  }
}
