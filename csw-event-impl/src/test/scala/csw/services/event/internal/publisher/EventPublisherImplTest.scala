package csw.services.event.internal.publisher

import java.io.IOException
import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}
import com.github.sebruck.EmbeddedRedis
import csw.messages.ccs.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.internal.{EventServiceCodec, ScalaRedisDriver, TestRedisPubSubListener}
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

class EventPublisherImplTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll with EmbeddedRedis {

  implicit val actorSystem: ActorSystem = ActorSystem()
  private val materializerSettings      = ActorMaterializerSettings(actorSystem).withInputBuffer(1, 1)
  implicit val mat: Materializer        = ActorMaterializer(materializerSettings)
  implicit val ec: ExecutionContext     = actorSystem.dispatcher

  var redis: RedisServer = _
  var redisPort: Int     = _

  lazy val redisURI: RedisURI       = RedisURI.create("localhost", redisPort)
  lazy val redisClient: RedisClient = RedisClient.create(redisURI)

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
    actorSystem.terminate()
  }

  test("publish to redis") {
    val prefix    = Prefix("test.prefix")
    val eventName = EventName("system")
    val event     = SystemEvent(prefix, eventName)
    val pbEvent   = Event.typeMapper.toBase(event)
    val eventKey  = event.eventKey.toString

    val pubSubConnection: StatefulRedisPubSubConnection[String, PbEvent] =
      redisClient.connectPubSub(EventServiceCodec, redisURI)
    val basicConnection: StatefulRedisConnection[String, PbEvent] = redisClient.connect(EventServiceCodec, redisURI)
    val pubSubCommands: RedisPubSubCommands[String, PbEvent]      = pubSubConnection.sync()
    val basicCommands: RedisCommands[String, PbEvent]             = basicConnection.sync()
    val listener                                                  = new TestRedisPubSubListener

    pubSubCommands.subscribe(eventKey)
    pubSubConnection.addListener(listener)

    val redisDriver = new EventPublisherImpl(new ScalaRedisDriver(redisClient, redisURI))
    Await.result(redisDriver.publish(event), 5.seconds)

    listener.messages should contain(pbEvent)
    basicCommands.get(eventKey) shouldBe pbEvent
  }
}
