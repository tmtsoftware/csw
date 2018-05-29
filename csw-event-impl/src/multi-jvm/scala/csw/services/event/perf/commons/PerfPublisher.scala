package csw.services.event.perf.commons

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.services.event.perf.utils.EventUtils
import csw.services.event.perf.utils.EventUtils._
import csw.services.event.perf.wiring.{TestConfigs, TestWiring}
import csw.services.event.scaladsl.EventPublisher

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class PerfPublisher(
    publishKey: String,
    eventsSetting: EventsSetting,
    testConfigs: TestConfigs,
    testWiring: TestWiring,
    sharedPublisher: EventPublisher
) {
  import eventsSetting._
  import testConfigs._
  import testWiring.wiring._

  private val totalMessages        = totalTestMsgs + warmup + 1 //inclusive of end-event
  private val payload: Array[Byte] = ("0" * payloadSize).getBytes("utf-8")

  private val publisher: EventPublisher =
    if (shareConnection) sharedPublisher else testWiring.publisher

  private val endEvent                 = event(EventName(s"${EventUtils.endEventS}-$publishKey"))
  private val eventName                = EventName(s"$testEventS-$publishKey")
  private var eventId                  = 0
  private var cancellable: Cancellable = _
  private var remaining: Long          = totalMessages

  private def eventGenerator(): Event = {
    eventId += 1
    // send extra two end envents in case one goes missing
    // subscriber stops listening on receiving firs end event, hence not affected by publisher publishing multiple end events
    if (eventId > totalMessages + 2) cancellable.cancel()
    if (eventId < totalMessages) event(eventName, eventId, payload) else endEvent
  }

  private def source(eventName: EventName): Source[SystemEvent, Future[Done]] =
    Source(1L to totalMessages)
      .map { id ⇒
        event(eventName, id, payload)
      }
      .watchTermination()(Keep.right)

  def startPublishingWithEventGenerator(): Unit = { cancellable = publisher.publish(eventGenerator(), (1000 / rate).millis) }

  def startPublishingWithSource(): Future[Done] =
    for {
      _   ← publisher.publish(source(EventName(s"$testEventS-$publishKey")))
      end ← publisher.publish(event(EventName(s"$endEventS-$publishKey")))
    } yield end

  def startPublishingInBatches(): Future[Done] = async {

    while (eventId < totalMessages) {
      val batchSize = Math.min(burstSize, remaining)
      var i         = 0

      var batchCompletionF: List[Future[Done]] = Nil

      while (i < batchSize) {
        batchCompletionF = publisher.publish(event(eventName, eventId, payload)) :: batchCompletionF
        eventId += 1
        i += 1
      }
      remaining = remaining - batchSize
      await(Future.sequence(batchCompletionF))
    }

    await(publisher.publish(event(EventName(s"$endEventS-$publishKey"))))
  }

}
