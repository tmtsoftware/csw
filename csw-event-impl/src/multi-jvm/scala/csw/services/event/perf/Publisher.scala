package csw.services.event.perf

import akka.actor.Cancellable
import csw.messages.events.{Event, EventName}
import csw.services.event.perf.EventUtils._
import csw.services.event.scaladsl.EventPublisher

class Publisher(testSettings: TestSettings, testConfigs: TestConfigs, id: Int, testWiring: TestWiring) {
  import testConfigs._
  import testSettings._

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

  def startPublishing(): Unit = { cancellable = publisher.publish(eventGenerator(), publishFrequency) }

}
