package csw.services.event.internal.api

import akka.Done
import akka.stream.KillSwitch
import akka.stream.scaladsl.Source
import csw.messages.ccs.events.{Event, EventKey}

import scala.concurrent.Future

trait EventPublishDriver {
  def publish(eventKey: EventKey, event: Event): Future[Done]
  def set(eventKey: EventKey, event: Event): Future[Done]
}

trait EventSubscriberDriver {
  def subscribe(eventKeys: Seq[EventKey]): Source[EventMessage[EventKey, Event], KillSwitch]
  def unsubscribe(eventKeys: Seq[EventKey]): Future[Done]
}

trait EventSubscriberDriverFactory {
  def make(): EventSubscriberDriver
}
