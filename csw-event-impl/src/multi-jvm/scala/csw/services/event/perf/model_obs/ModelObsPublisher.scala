package csw.services.event.perf.model_obs

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.services.event.perf.utils.EventUtils
import csw.services.event.perf.utils.EventUtils._
import csw.services.event.perf.wiring.{TestConfigs, TestWiring}
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class ModelObsPublisher(
    publishKey: String,
    pubSetting: PubSetting,
    testConfig: TestConfigs,
    testWiring: TestWiring,
    sharedPublisher: EventPublisher
) {
  import pubSetting._
  import testWiring.wiring._

  private val totalMessages        = totalTestMsgs + warmup + 1 //inclusive of end-event
  private val payload: Array[Byte] = ("0" * payloadSize).getBytes("utf-8")

  import testConfig._
  private val publisher: EventPublisher = if (shareConnection) sharedPublisher else testWiring.publisher

  private val endEvent                 = event(EventName(s"${EventUtils.endEventS}-$publishKey"))
  private val eventName                = EventName(s"$testEventS-$publishKey")
  private var eventId                  = 0
  private var cancellable: Cancellable = _

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

  def startPublishingWithEventGenerator(): Unit = { cancellable = publisher.publish(eventGenerator(), (1000 / rate).millis) }

  def startPublishingWithSource(): Future[Done] =
    for {
      _   ← publisher.publish(source(EventName(s"$testEventS-$publishKey")))
      end ← publisher.publish(event(EventName(s"$endEventS-$publishKey")))
    } yield end
}
