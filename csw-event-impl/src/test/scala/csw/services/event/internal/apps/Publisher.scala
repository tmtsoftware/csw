package csw.services.event.internal.apps

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ThrottleMode}
import csw.messages.events.Event
import csw.messages.params.generics.Key
import csw.messages.params.generics.KeyType.LongKey
import csw.services.event.helpers.Utils.makeEvent
import csw.services.event.internal.apps.Publisher.mock
import csw.services.event.internal.commons.Wiring
import csw.services.event.scaladsl.RedisFactory
import csw.services.location.scaladsl.LocationService
import io.lettuce.core.RedisClient
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

object Publisher extends App with MockitoSugar {
  private val actorSystem: ActorSystem = ActorSystem()
  private val p: ActorRef              = actorSystem.actorOf(Props(classOf[P]), "P")
  p ! "start"

}

class P extends Actor {
  private implicit val mat: ActorMaterializer = ActorMaterializer()
  private implicit val ec: ExecutionContext   = context.dispatcher

  private val redisClient  = RedisClient.create()
  private val wiring       = new Wiring(context.system)
  private val redisFactory = new RedisFactory(redisClient, mock[LocationService], wiring)
  private val publisher    = redisFactory.publisher("localhost", 6379)

  val timeNanosKey: Key[Long] = LongKey.make("eventTime")

  val source: Source[Event, NotUsed] = Source(0 until 10000)
    .throttle(300, 1.second, 300, ThrottleMode.Shaping)
    .map { counter ⇒
      makeEvent(counter)
    }

  override def receive: Receive = {
    case "start" ⇒ publisher.publish(source)

  }
}
