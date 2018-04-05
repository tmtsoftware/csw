package csw.services.event.internal.apps

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import csw.messages.events.{Event, EventKey, SystemEvent}
import csw.messages.params.generics.Key
import csw.messages.params.generics.KeyType.LongKey
import csw.services.event.helpers.Utils.makeEvent
import csw.services.event.internal.apps.Subscriber.mock
import csw.services.event.internal.commons.Wiring
import csw.services.event.scaladsl.{EventSubscriber, RedisFactory}
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext

object Subscriber extends App with MockitoSugar {
  private val actorSystem: ActorSystem = ActorSystem()
  val n                                = 1
  (0 to n).foreach { i ⇒
    actorSystem.actorOf(Props(classOf[S]), s"S$i")
  }
}

class S extends Actor {
  private implicit val mat: ActorMaterializer = ActorMaterializer()
  private implicit val ec: ExecutionContext   = context.dispatcher

  private val redisClient     = RedisClient.create()
  private val wiring          = new Wiring(context.system)
  private val redisFactory    = new RedisFactory(redisClient, mock[LocationService], wiring)
  val timeNanosKey: Key[Long] = LongKey.make("eventTime")

  val subscriber: EventSubscriber = redisFactory.subscriber("localhost", 6379)
  val key: EventKey               = makeEvent(0).eventKey

  subscriber
    .subscribeCallback(Set(key), onEvent)

  def onEvent(event: Event): Unit = event match {
    case e: SystemEvent ⇒
      val l = System.nanoTime() - e.get(timeNanosKey).get.head
      println(s"${l / 1000}")
  }

  override def receive: Receive = {
    case _ ⇒
  }
}
