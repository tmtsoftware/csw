package csw.services.event.perf.apps

import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Keep, Source}
import com.typesafe.config.Config
import csw.messages.events.{EventName, SystemEvent}
import csw.services.event.perf.EventUtils._
import csw.services.event.perf.{TestSettings, TestWiring}
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._

class SimplePublisher(testSettings: TestSettings, id: Int)(implicit val system: ActorSystem) {
  import testSettings._

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private val wiring                    = new TestWiring(system)
  private val payload: Array[Byte]      = ("0" * payloadSize).getBytes("utf-8")
  private val publisher: EventPublisher = wiring.publisher

  private val config: Config          = system.settings.config
  private val throttlingElements: Int = config.getInt("csw.test.EventThroughputSpec.throttling.elements")
  private val throttlingDuration: FiniteDuration = {
    val d = config.getDuration("csw.test.EventThroughputSpec.throttling.per")
    FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS)
  }
  private val warmup: Int = config.getInt("csw.test.EventThroughputSpec.warmup")

  private def source(eventName: EventName): Source[SystemEvent, Future[Done]] =
    Source(1L to totalMessages + warmup)
      .throttle(throttlingElements, throttlingDuration, throttlingElements, ThrottleMode.shaping)
      .map { id ⇒
        eventWithNanos(eventName, id, payload)
      }
      .watchTermination()(Keep.right)

  def start(): Future[Done] =
    for {
      _   ← publisher.publish(source(EventName(s"$testEvent-$id")))
      end ← publisher.publish(event(EventName(s"$endEventS-$id")))
    } yield end

}
