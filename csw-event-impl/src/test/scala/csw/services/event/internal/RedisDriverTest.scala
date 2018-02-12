package csw.services.event.internal

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.github.sebruck.EmbeddedRedis
import csw.messages.ccs.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw_protobuf.events.PbEvent
import io.lettuce.core._
import io.lettuce.core.pubsub.{RedisPubSubListener, StatefulRedisPubSubConnection}
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationDouble

class RedisDriverTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll with EmbeddedRedis {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer        = ActorMaterializer()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

  test("publish to redis") {
    val prefix    = Prefix("test.prefix")
    val eventName = EventName("system")
    val event     = SystemEvent(prefix, eventName)
    val pbEvent   = Event.typeMapper.toBase(event)
    val eventKey  = event.eventKey.toString

    withRedis() { port ⇒
      val redisURI                                                   = RedisURI.create("localhost", port)
      val redisClient                                                = RedisClient.create(redisURI)
      val connection: StatefulRedisPubSubConnection[String, PbEvent] = redisClient.connectPubSub(EventServiceCodec, redisURI)
      val listener                                                   = new TestRedisPubSubListener
      val pubSubCommands: RedisPubSubCommands[String, PbEvent]       = connection.sync()

      pubSubCommands.subscribe(eventKey)

      connection.addListener(listener)
    }

//    val unit = Await.result(new RedisDriver(redisClient, redisURI).publish(eventKey, pbEvent), 5.seconds)

  }

}
