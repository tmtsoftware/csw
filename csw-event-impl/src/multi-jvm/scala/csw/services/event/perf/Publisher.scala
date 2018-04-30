package csw.services.event.perf

import akka.Done
import akka.actor.Cancellable
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events.{Event, EventName, SystemEvent}
import csw.services.event.perf.EventUtils._
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.Future

class Publisher(testSettings: TestSettings, testConfigs: TestConfigs, id: Int, testWiring: TestWiring) {
  import testConfigs._
  import testSettings._
  import testWiring.wiring._

  private val totalMessages             = totalTestMsgs + warmupMsgs + 1 //inclusive of end-event
  private val payload: Array[Byte]      = ("0" * payloadSize).getBytes("utf-8")
  private val publisher: EventPublisher = testWiring.publisher
  private val endEvent                  = event(EventName(s"${EventUtils.endEventS}-$id"))
  private val eventName                 = EventName(s"$testEventS-$id")
  private var counter                   = 0
  private var cancellable: Cancellable  = _

  private def eventGenerator(): Event = {
    counter += 1
    if (counter > totalMessages) cancellable.cancel()
    if (counter < totalMessages) event(eventName, counter, payload) else endEvent
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

}
