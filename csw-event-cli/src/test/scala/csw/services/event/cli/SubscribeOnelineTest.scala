package csw.services.event.cli

import akka.stream.Materializer
import csw.messages.events._
import csw.messages.params.models.Id
import csw.services.event.cli.BufferExtensions.RichBuffer
import csw.services.event.helpers.TestFutureExt.RichFuture
import org.scalatest.concurrent.Eventually
import org.scalatest.time.SpanSugar.convertDoubleToGrainOfTime
import org.scalatest.{FunSuite, Matchers}

import scala.collection.{immutable, mutable}
import scala.concurrent.ExecutionContext

class SubscribeOnelineTest extends FunSuite with Matchers with SeedData with Eventually {

  def events(name: EventName): immutable.Seq[Event] = for (i ← 1 to 10) yield event1.copy(eventId = Id(i.toString))

  class EventGenerator(eventName: EventName) {
    var counter                               = 0
    var publishedEvents: mutable.Queue[Event] = mutable.Queue.empty
    val eventsGroup: immutable.Seq[Event]     = events(eventName)

    def generate: Event = {
      val event = eventsGroup(counter)
      counter += 1
      publishedEvents.enqueue(event)
      event
    }
  }

  import cliWiring._

  test("should be able to subscribe and get oneline output to event key") {

    implicit val mat: Materializer    = actorRuntime.mat
    implicit val ec: ExecutionContext = actorRuntime.ec

    val eventGenerator = new EventGenerator(EventName(s"system_1"))
    import eventGenerator._
    val publisher   = eventService.defaultPublisher.await
    val cancellable = publisher.publish(eventGenerator.generate, 400.millis)

    val eventKey               = eventsGroup.head.eventKey
    val options                = Options(cmd = "subscribe", eventKeys = Seq(eventKey))
    val (subscriptionF, doneF) = commandLineRunner.subscribe(options)

    Thread.sleep(1000)

    cancellable.cancel()
    subscriptionF.map(_.unsubscribe())

    logBuffer shouldEqualContentsOf "subscribe/expected/outOneline.txt"
  }

}
