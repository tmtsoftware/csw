package csw.services.event.scaladsl

import akka.Done
import akka.stream.scaladsl.Source
import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.api.reactive.ChannelMessage

import scala.concurrent.Future

trait EventServiceDriver {
  def publish(key: String, data: PbEvent): Future[Done]
  def set(key: String, data: PbEvent): Future[Done]
  def subscribe(keys: Seq[String]): Source[ChannelMessage[String, PbEvent], Future[Done]]
  def unsubscribe(keys: Seq[String]): Future[Done]
}
