package csw.event.client.perf

import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, typed}
import akka.stream.Materializer
import csw.event.client.EventServiceFactory
import csw.event.client.helpers.Utils
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.Event
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class InitialPublishLatencyTest extends FunSuite with BeforeAndAfterAll {

  private implicit val system: ActorSystem               = ActorSystem()
  private implicit val typedSystem: typed.ActorSystem[_] = system.toTyped
  private implicit val mat: Materializer                 = Materializer(typedSystem)
  private val ls: LocationService                        = HttpLocationServiceFactory.makeLocalClient
  private val eventServiceFactory                        = new EventServiceFactory().make(ls)
  import eventServiceFactory._

  ignore("should not incurr high latencies for initially published events") {
    val event = Utils.makeEvent(0)

    defaultSubscriber.subscribeCallback(Set(event.eventKey), report)

    (0 to 500).foreach { id =>
      defaultPublisher.publish(Utils.makeEvent(id))
      Thread.sleep(10)
    }

    def report(event: Event): Unit = println(System.currentTimeMillis() - event.eventTime.value.toEpochMilli)
  }

}
