package csw.services.event

import akka.Done
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events._
import csw.messages.framework.ComponentInfo
import csw.messages.params.models.{Id, Prefix}
import csw.services.event.api.exceptions.PublishFailure
import csw.services.event.api.scaladsl.EventService
import csw.services.logging.scaladsl.Logger

import scala.async.Async._
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContext, Future}

class EventPublishExamples(
    eventService: EventService,
    log: Logger
)(implicit ec: ExecutionContext) {

  def singleEvent(componentInfo: ComponentInfo): Future[Unit] =
    //#single-event
    async {
      val publisher = await(eventService.defaultPublisher)
      val event     = SystemEvent(componentInfo.prefix, EventName("filter_wheel"))
      publisher.publish(event)
    }
  //#single-event

  def source(componentInfo: ComponentInfo): Unit = {
    val n: Int                                             = 10
    def makeEvent(i: Int, prefix: Prefix, name: EventName) = SystemEvent(prefix, name)

    //#with-source
    async {
      val publisher = await(eventService.defaultPublisher)
      val eventStream: Source[Event, Future[Done]] = Source(1 to n)
        .map(id ⇒ makeEvent(id, componentInfo.prefix, EventName("filter_wheel")))
        .watchTermination()(Keep.right)

      publisher.publish(eventStream, failure ⇒ onError(failure))
    }

    def onError(publishFailure: PublishFailure): Unit =
      log.error(s"Publish failed for event: [${publishFailure.event}]", ex = publishFailure.cause)
    //#with-source
  }

  def generator(componentInfo: ComponentInfo): Future[Unit] =
    //#event-generator
    async {
      val publisher        = await(eventService.defaultPublisher)
      val baseEvent: Event = SystemEvent(componentInfo.prefix, EventName("filter_wheel"))
      val interval         = 100.millis

      publisher.publish(eventGenerator(), interval)

      // this holds the logic for event generation, could be based on some computation or current state of HCD
      def eventGenerator(): Event = baseEvent match {
        case e: SystemEvent  ⇒ e.copy(eventId = Id(), eventTime = EventTime())
        case e: ObserveEvent ⇒ e.copy(eventId = Id(), eventTime = EventTime())
      }
    }
  //#event-generator
}
