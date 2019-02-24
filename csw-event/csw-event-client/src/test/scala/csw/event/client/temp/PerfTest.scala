package csw.event.client.temp

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import csw.event.api.scaladsl.{EventPublisher, EventSubscriber}
import csw.event.client.EventServiceFactory
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.helpers.Utils
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.events.Event
import org.scalatest.FunSuite

class PerfTest extends FunSuite {

  private implicit val system: ActorSystem    = ActorSystem()
  private implicit val mat: ActorMaterializer = ActorMaterializer()
  private val ls: LocationService             = HttpLocationServiceFactory.makeLocalClient
  private val factory                         = new EventServiceFactory().make(ls)
  private val id                              = 0
  private val event                           = Utils.makeEvent(id)

  val subscriber: EventSubscriber = factory.defaultSubscriber
  val publisher: EventPublisher   = factory.defaultPublisher

  ignore("asd") {
    publisher.publish(Utils.makeEvent(10)).await

    subscriber.subscribeCallback(Set(event.eventKey), report)

    while (true) {
      publisher.publish(Utils.makeEvent(id))
      Thread.sleep(10)
    }
  }

  private def report(event: Event): Unit = {
    val currentTime = System.currentTimeMillis()
    val origTime    = event.eventTime.value.toEpochMilli

    println(currentTime - origTime)
  }
}
