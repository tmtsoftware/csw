package csw.services.event.internal.redis

import akka.testkit.TestProbe
import com.github.sebruck.EmbeddedRedis
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.PortHelper
import csw.services.event.internal.Wiring
import csw.services.event.internal.pubsub.{EventPublisherImpl, EventSubscriberImpl}
import csw_protobuf.events.PbEvent
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class EventServiceTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {

  private val port: Port = PortHelper.freePort
  private val wiring     = new Wiring(port)
  println(s"starting redis on $port")

  import wiring._
  lazy val redis: RedisServer = RedisServer.builder().setting("bind 127.0.0.1").port(port).build()

  redis.start()

  val prefix             = Prefix("test.prefix")
  val eventName          = EventName("system")
  val event              = SystemEvent(prefix, eventName)
  val pbEvent: PbEvent   = Event.typeMapper.toBase(event)
  val eventKey: EventKey = event.eventKey

  override def afterAll(): Unit = {
    Await.result(redisClient.shutdownAsync().toCompletableFuture.toScala, 5.seconds)
    Await.result(actorSystem.terminate(), 5.seconds)
    redis.stop()
  }

  test("test subscribe with callback") {
    val testProbe = TestProbe()

    val subscriptionImpl: EventSubscriberImpl = new EventSubscriberImpl(new RedisEventBusDriver(redisClient, redisURI))

    subscriptionImpl.subscribe(Seq(eventKey), e ⇒ testProbe.ref ! e)

    val publisherImpl = new EventPublisherImpl(new RedisEventBusDriver(redisClient, redisURI))
    Await.result(publisherImpl.publish(event), 5.seconds)

    testProbe.expectMsg(event)
  }

  test("pub-sub") {
    subscriberImpl.subscribe(Seq(eventKey), e => println(e))
    publisherImpl
  }

}
