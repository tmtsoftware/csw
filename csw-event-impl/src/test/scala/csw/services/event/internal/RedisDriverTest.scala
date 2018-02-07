package csw.services.event.internal

import java.net.SocketAddress
import java.util.concurrent.CompletableFuture

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.messages.ccs.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw_protobuf.events.PbEvent
import io.lettuce.core._
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

class RedisDriverTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val mat: Materializer        = ActorMaterializer()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

  test("publish to redis") {
    val prefix          = Prefix("test.prefix")
    val eventName       = EventName("system")
    val event           = SystemEvent(prefix, eventName)
    val pbEvent         = Event.typeMapper.toBase(event)
    val eventKey        = event.eventKey.toString
    val redisURI        = RedisURI.create("redis://test")
    val mockRedisClient = mock[RedisClient]
    val mockCommands    = mock[RedisPubSubAsyncCommands[String, PbEvent]]
    val mockConnection  = mock[StatefulRedisPubSubConnection[String, PbEvent]]
    val mockConnectionFuture = ConnectionFuture.from(
      mock[SocketAddress],
      CompletableFuture.completedFuture(mockConnection)
    )

    when(mockRedisClient.connectPubSubAsync(EventServiceCodec, redisURI)).thenReturn(mockConnectionFuture)
    when(mockConnection.async()).thenReturn(mockCommands)

    when(mockCommands.publish(any[String], any[PbEvent])).thenReturn(mock[RedisFuture[java.lang.Long]])
    when(mockCommands.set(any[String], any[PbEvent])).thenReturn(mock[RedisFuture[String]])

    new RedisDriver(mockRedisClient, redisURI, EventServiceCodec).publish(eventKey, pbEvent)
    verify(mockCommands).publish(eventKey, pbEvent)
  }

}
