package example.event

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, Source}
import csw.params.events._
import csw.command.client.models.framework.ComponentInfo
import csw.params.core.models.{Id, ObsId}
import csw.event.api.exceptions.PublishFailure
import csw.event.api.scaladsl.EventService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class EventPublishExamples(eventService: EventService, log: Logger) {

  def singleEvent(componentInfo: ComponentInfo): Future[Done] =
    //#single-event
    {
      val publisher = eventService.defaultPublisher
      val event     = SystemEvent(componentInfo.prefix, EventName("filter_wheel"))
      publisher.publish(event)
    }
  //#single-event

  def source(componentInfo: ComponentInfo): Future[Done] = {
    val n: Int                                             = 10
    def makeEvent(i: Int, prefix: Prefix, name: EventName) = SystemEvent(prefix, name)

    //#with-source
    def onError(publishFailure: PublishFailure): Unit =
      log.error(s"Publish failed for event: [${publishFailure.event}]", ex = publishFailure.cause)

    val publisher = eventService.defaultPublisher
    val eventStream: Source[Event, Future[Done]] = Source(1 to n)
      .map(id => makeEvent(id, componentInfo.prefix, EventName("filter_wheel")))
      .watchTermination()(Keep.right)

    publisher.publish(eventStream, failure => onError(failure))
    //#with-source
  }

  def generator(componentInfo: ComponentInfo): Cancellable =
    //#event-generator
    {
      val publisher        = eventService.defaultPublisher
      val baseEvent: Event = SystemEvent(componentInfo.prefix, EventName("filter_wheel"))
      val interval         = 100.millis

      // this holds the logic for event generation, could be based on some computation or current state of HCD
      def eventGenerator(): Option[Event] =
        baseEvent match {
          case e: SystemEvent  => Some(SystemEvent(e.source, e.eventName, e.paramSet))
          case e: ObserveEvent => Some(IRDetectorEvent.observeStart(e.source, ObsId("2020A-001-123")))
        }

      publisher.publish(eventGenerator(), interval)
    }
  //#event-generator
}
