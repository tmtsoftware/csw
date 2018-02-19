package csw.services.event.internal.pubsub

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import csw.messages.ccs.events.Event
import csw.services.event.internal.api.EventPublishDriver
import csw.services.event.scaladsl.EventPublisher
import csw_protobuf.events.PbEvent
import async.Async._

import scala.concurrent.Future

class EventPublisherImpl(eventPublishDriver: EventPublishDriver)(implicit system: ActorSystem) extends EventPublisher {

  private val settings                        = ActorMaterializerSettings(system).withSupervisionStrategy(Supervision.getResumingDecider)
  private implicit lazy val mat: Materializer = ActorMaterializer(settings)

  import system.dispatcher

  override def publish(source: Source[Event, NotUsed]): Future[Done] = source.mapAsync(1)(publish).runWith(Sink.ignore)

  override def publish(event: Event): Future[Done] = async {
    val key: String    = event.eventKey.key
    val value: PbEvent = Event.typeMapper.toBase(event)
    await(eventPublishDriver.publish(key, value))
    await(eventPublishDriver.set(key, value))
  }
}
