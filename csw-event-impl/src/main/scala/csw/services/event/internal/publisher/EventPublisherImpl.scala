package csw.services.event.internal.publisher

import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.{EventPublisher, EventServiceDriver}
import csw_protobuf.events.PbEvent

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

class EventPublisherImpl(eventServiceDriver: EventServiceDriver)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventPublisher {

  private lazy val eventQueue: SourceQueueWithComplete[StreamElement] = Source
    .queue[StreamElement](4096, OverflowStrategy.dropHead)
    .groupBy(1024, _.key)
    .mapAsync(1) { element ⇒
      eventServiceDriver
        .publish(element.key, element.value)
        .flatMap(_ ⇒ eventServiceDriver.set(element.key, element.value))
        .transformWith(element.complete)
    }
    .to(Sink.ignore)
    .run()

  override def publish(event: Event): Future[Unit] = {
    val element = StreamElement(event)
    eventQueue.offer(element)
    element.future
  }

  private case class StreamElement(event: Event) {
    val p: Promise[Unit]     = Promise[Unit]
    val key: String          = event.eventKey.toString
    val value: PbEvent       = Event.typeMapper.toBase(event)
    val future: Future[Unit] = p.future

    def complete(t: Try[Unit]): Future[Unit] = p.complete(t).future
  }

}
