package csw.services.event.internal.redis

import java.util.concurrent.{CompletableFuture, CompletionStage}

import akka.actor.ActorSystem
import csw.messages.events.Event
import csw.services.event.helpers.Utils
import csw.services.event.internal.pubsub.{EventPublisherUtil, EventSubscriberUtil}
import csw.services.event.internal.wiring.{EventServiceResolver, Wiring}
import csw.services.event.scaladsl.{EventSubscription, RedisSentinelFactory}
import io.lettuce.core.{ClientOptions, RedisClient}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt

object Demo1 extends MockitoSugar {
  private val redisPort = 26379

  private implicit val actorSystem: ActorSystem = ActorSystem()
  private val wiring                            = new Wiring(actorSystem)
  import wiring._
  private val redisClient = RedisClient.create()
  private val builder: ClientOptions =
    ClientOptions.builder().disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS).build()
  redisClient.setOptions(builder)
  private val redisFactory =
    new RedisSentinelFactory(redisClient, mock[EventServiceResolver], new EventPublisherUtil(), new EventSubscriberUtil())

  var counter = 0

  def eventGenerator(): Event = {
    counter += 1
    Utils.makeEvent(counter)
  }

  val event = Utils.makeEvent(0)

  //  private val publisher    = redisFactory.publisher("localhost", redisPort)
  //  private val subscriber   = redisFactory.subscriber("localhost", redisPort)
  private val publisher  = redisFactory.publisher("localhost", redisPort, "mymaster")
  private val subscriber = redisFactory.subscriber("localhost", redisPort, "mymaster")

  publisher.publish(eventGenerator, 1.seconds, e ⇒ println("errr"))

  private val subscription: EventSubscription = subscriber.subscribeCallback(Set(event.eventKey), e ⇒ println(e))
}

object demo2 extends App {
  private val value: CompletableFuture[Int] = new CompletableFuture[Int] {
    throw new RuntimeException("dummy")
  }
  value

  Thread.sleep(1000)
}
