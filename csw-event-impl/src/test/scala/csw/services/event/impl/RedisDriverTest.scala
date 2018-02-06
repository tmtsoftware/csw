package csw.services.event.impl

import java.net.SocketAddress
import java.util.concurrent.CompletableFuture

import akka.actor.ActorSystem
import csw.messages.ccs.events.{Event, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw_protobuf.events.PbEvent
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands
import io.lettuce.core.{ConnectionFuture, RedisClient, RedisURI}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import reactor.core.publisher.Mono
import scala.concurrent.ExecutionContext

class RedisDriverTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext     = actorSystem.dispatcher

  override def afterAll(): Unit = {
    actorSystem.terminate()
  }

  test("publish to redis") {
    val prefix            = "test.prefix"
    val eventName         = "system"
    val event             = SystemEvent(Prefix(prefix), EventName(eventName))
    val eventServiceCodec = new EventServiceCodec
    val redisURI          = RedisURI.create("redis://test")
    val mockRedisClient   = mock[RedisClient]
    val mockCommands      = mock[RedisPubSubReactiveCommands[String, PbEvent]]
    val mockMono          = mock[Mono[java.lang.Long]]
    val mockConnection    = mock[StatefulRedisPubSubConnection[String, PbEvent]]
    val mockConnectionFuture = ConnectionFuture.from(
      mock[SocketAddress],
      CompletableFuture.completedFuture(mockConnection)
    )

    when(mockRedisClient.connectPubSubAsync(eventServiceCodec, redisURI)).thenReturn(mockConnectionFuture)
    when(mockConnection.reactive()).thenReturn(mockCommands)
    when(mockCommands.publish(any[String], any[PbEvent])).thenReturn(mockMono)

    new RedisDriver(mockRedisClient, redisURI, eventServiceCodec).publish("testChannel", Event.typeMapper.toBase(event))
    verify(mockCommands).publish(any[String], any[PbEvent])
  }
}
