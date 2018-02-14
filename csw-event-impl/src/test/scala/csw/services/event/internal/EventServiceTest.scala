package csw.services.event.internal

import java.io.IOException
import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestProbe
import com.github.sebruck.EmbeddedRedis
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.internal.publisher.EventPublisherImpl
import csw.services.event.internal.subscriber.EventSubscriptionImpl
import csw_protobuf.events.PbEvent
import io.lettuce.core._
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.annotation.tailrec
import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success, Try}

class EventServiceTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll with EmbeddedRedis {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer        = ActorMaterializer()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher

  var redis: RedisServer = _
  var redisPort: Int     = _

  lazy val redisURI: RedisURI       = RedisURI.create("localhost", redisPort)
  lazy val redisClient: RedisClient = RedisClient.create(redisURI)

  val prefix             = Prefix("test.prefix")
  val eventName          = EventName("system")
  val event              = SystemEvent(prefix, eventName)
  val pbEvent: PbEvent   = Event.typeMapper.toBase(event)
  val eventKey: EventKey = event.eventKey

  override def beforeAll(): Unit = {
    redisPort = freePort
    redis = RedisServer.builder().setting("bind 127.0.0.1").port(redisPort).build()
    redis.start()
  }

  @tailrec
  private final def freePort: Int = {
    Try(new ServerSocket(0)) match {
      case Success(socket) =>
        val port = socket.getLocalPort
        socket.close()
        port
      case Failure(e: IOException) ⇒ freePort
      case Failure(e)              ⇒ throw e
    }
  }

  override def afterAll(): Unit = {
    redis.stop()
    Await.result(actorSystem.terminate(), 5.seconds)
  }

  test("test subscribe with callback") {
    val testProbe = TestProbe()

    val subscriptionImpl: EventSubscriptionImpl =
      new EventSubscriptionImpl(new ScalaRedisDriver(redisClient, redisURI), e ⇒ testProbe.ref ! e, Seq(eventKey))

    subscriptionImpl.subscribe()

    val publisherImpl = new EventPublisherImpl(new ScalaRedisDriver(redisClient, redisURI))
    Await.result(publisherImpl.publish(event), 5.seconds)

    testProbe.expectMsg(event)
  }
}
