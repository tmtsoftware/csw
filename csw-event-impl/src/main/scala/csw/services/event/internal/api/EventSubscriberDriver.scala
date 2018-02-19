package csw.services.event.internal.api

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw_protobuf.events.PbEvent

import scala.concurrent.Future

trait EventSubscriberDriver {
  def subscribe(keys: Seq[String]): Source[EventMessage[String, PbEvent], KillSwitch]
  def unsubscribe(keys: Seq[String]): Future[Done]
}

trait EventPublishDriver {
  def publish(key: String, data: PbEvent): Future[Done]
  def set(key: String, data: PbEvent): Future[Done]
}
