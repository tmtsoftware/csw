package csw.services.event.perf

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.services.event.perf.EventUtils._
import csw.services.event.scaladsl.EventPublisher

import scala.async.Async.{async, await}
import scala.concurrent.Future

class Publisher(
    testSettings: TestSettings,
    testConfigs: TestConfigs,
    id: Int,
    testWiring: TestWiring,
    sharedPublisher: EventPublisher
) {
  import testConfigs._
  import testSettings._
  import testWiring.wiring._

  private val totalMessages        = totalTestMsgs + warmupMsgs + 1 //inclusive of end-event
  private val payload: Array[Byte] = ("0" * payloadSize).getBytes("utf-8")

  private val publisher: EventPublisher =
    if (shareConnection) sharedPublisher else testWiring.publisher

  private val endEvent                 = event(EventName(s"${EventUtils.endEventS}-$id"))
  private val eventName                = EventName(s"$testEventS-$id")
  private var eventId                  = 0
  private var cancellable: Cancellable = _
  private var remaining: Long          = totalMessages

  private def eventGenerator(): Event = {
    eventId += 1
    if (eventId > totalMessages) cancellable.cancel()
    if (eventId < totalMessages) event(eventName, eventId, payload) else endEvent
  }

  private def source(eventName: EventName): Source[SystemEvent, Future[Done]] =
    Source(1L to totalMessages)
      .map { id ⇒
        event(eventName, id, payload)
      }
      .watchTermination()(Keep.right)

  def startPublishingWithEventGenerator(): Unit = { cancellable = publisher.publish(eventGenerator(), publishFrequency) }

  def startPublishingWithSource(): Future[Done] =
    for {
      _   ← publisher.publish(source(EventName(s"$testEventS-$id")))
      end ← publisher.publish(event(EventName(s"$endEventS-$id")))
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

    await(publisher.publish(event(EventName(s"$endEventS-$id"))))
  }

}
