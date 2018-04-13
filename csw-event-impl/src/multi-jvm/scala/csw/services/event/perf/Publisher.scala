package csw.services.event.perf

import akka.actor.{ActorSystem, Cancellable}
import csw.messages.events.{Event, EventName}
import csw.services.event.perf.EventUtils._
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

class Publisher(testSettings: TestSettings, testConfigs: TestConfigs, id: Int)(implicit val system: ActorSystem) {
  import testConfigs._
  import testSettings._

  private implicit val ec: ExecutionContext = system.dispatcher
  private val wiring                        = new TestWiring(system)

  private val totalMessages             = totalTestMsgs + warmupMsgs + 1 //inclusive of end-event
  private val payload: Array[Byte]      = ("0" * payloadSize).getBytes("utf-8")
  private val publisher: EventPublisher = wiring.publisher
  private val endEvent                  = event(EventName(s"${EventUtils.endEventS}-$id"))
  private val eventName                 = EventName(s"$testEventS-$id")
  private var counter                   = 0
  private var cancellable: Cancellable  = _

  private def eventGenerator(): Event = {
    counter += 1
    if (counter > totalMessages) cancellable.cancel()
    if (counter < totalMessages) event(eventName, counter, payload) else endEvent
  }

  def startPublishing(): Unit = { cancellable = publisher.publish(eventGenerator, publishFrequency) }

}
