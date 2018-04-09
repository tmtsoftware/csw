package csw.services.event.internal.perf.apps

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ThrottleMode
import akka.stream.scaladsl.{Keep, Source}
import csw.messages.events.EventName
import EventUtils._
import csw.services.event.scaladsl.EventPublisher

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

object SimplePublisher extends App {

  implicit val system: ActorSystem          = ActorSystem("Pub")
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  private val wiring                        = new TestWiring(system)
  private val totalMsgs                     = 5000
  private val noOfPublishers                = 2

  val payload: Array[Byte] = ("0" * 100).getBytes("utf-8")

  private val publisher: EventPublisher = wiring.publisher

  private def warmupSource(eventName: EventName) =
    Source(1 to 1000)
      .throttle(100, 1.seconds, 100, ThrottleMode.shaping)
      .map { id ⇒
        event(eventName, id, payload)
      }
      .watchTermination()(Keep.right)

  private def source(eventName: EventName) =
    Source(1 to totalMsgs)
      .throttle(100, 1.seconds, 100, ThrottleMode.shaping)
      .map { id ⇒
        eventWithNanos(eventName, id, payload)
      }
      .watchTermination()(Keep.right)

  var completedF: List[Future[Done]] = Nil

  for (n ← 0 until noOfPublishers) {

    val doneF = for {
      _   ← publisher.publish(warmupSource(EventName(s"$testEvent-$n")))
      _   ← publisher.publish(event(EventName(s"$startEvent")))
      _   ← publisher.publish(source(EventName(s"$testEvent-$n")))
      end ← publisher.publish(event(EventName(s"$endEventS-$n")))
    } yield end

    completedF = doneF :: completedF
  }

  Await.result(Future.sequence(completedF), 15.minutes)
  system.terminate()
}
