package csw.services.event.internal.pubsub

import akka.Done
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import csw.messages.ccs.events.Event
import csw.services.event.internal.api.{EventPublishDriver, EventSubscriberDriver}
import csw.services.event.scaladsl.EventPublisher
import csw_protobuf.events.PbEvent

import scala.concurrent.{Future, Promise}
import scala.util.Try

class EventPublisherImpl(eventPublishDriver: EventPublishDriver)(implicit system: ActorSystem) extends EventPublisher {

  private val bufferSize    = 4096
  private val maxSubStreams = 1024

  private val settings                        = ActorMaterializerSettings(system).withSupervisionStrategy(Supervision.getResumingDecider)
  private implicit lazy val mat: Materializer = ActorMaterializer(settings)

  import system.dispatcher

  private val eventQueue: SourceQueueWithComplete[StreamElement] = Source
    .queue[StreamElement](bufferSize, OverflowStrategy.dropHead)
    .groupBy(maxSubStreams, _.key)
    .mapAsync(1) { element ⇒
      eventPublishDriver
        .publish(element.key, element.value)
        .flatMap(_ ⇒ eventPublishDriver.set(element.key, element.value))
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
    val key: String          = event.eventKey.key
    val value: PbEvent       = Event.typeMapper.toBase(event)
    val future: Future[Done] = p.future

    def complete(t: Try[Done]): Future[Done] = p.complete(t).future
  }

}
