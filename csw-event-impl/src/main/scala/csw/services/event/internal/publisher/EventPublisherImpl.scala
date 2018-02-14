package csw.services.event.internal.publisher

import akka.Done
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import csw.messages.ccs.events.Event
import csw.services.event.scaladsl.{EventPublisher, EventServiceDriver}
import csw_protobuf.events.PbEvent

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

class EventPublisherImpl(eventServiceDriver: EventServiceDriver)(implicit ec: ExecutionContext, mat: Materializer)
    extends EventPublisher {

  private val bufferSize    = 4096
  private val maxSubstreams = 1024

  private lazy val eventQueue: SourceQueueWithComplete[StreamElement] = Source
    .queue[StreamElement](bufferSize, OverflowStrategy.dropHead)
    .groupBy(maxSubstreams, _.key)
    .mapAsync(1) { element ⇒
      eventServiceDriver
        .publish(element.key, element.value)
        .flatMap(_ ⇒ eventServiceDriver.set(element.key, element.value))
        .transformWith(element.complete)
    }
    .to(Sink.ignore)
    .run()

  override def publish(event: Event): Future[Done] = {
    val element = new StreamElement(event)
    eventQueue.offer(element)
    element.future
  }

  private class StreamElement(event: Event) {
    val p: Promise[Done]     = Promise[Done]
    val key: String          = event.eventKey.toString
    val value: PbEvent       = Event.typeMapper.toBase(event)
    val future: Future[Done] = p.future

    def complete(t: Try[Done]): Future[Done] = p.complete(t).future
  }

}
