package csw.services.event.internal.redis

import akka.stream.scaladsl.{Keep, Sink}
import com.github.sebruck.EmbeddedRedis
import csw.messages.ccs.events.{EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.helpers.PortHelper
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.Wiring
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class RedisEventSubscriberDriverTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {

  private val port: Port = PortHelper.freePort
  private val wiring     = new Wiring(port)

  import wiring._
  lazy val redis: RedisServer = RedisServer.builder().setting("bind 127.0.0.1").port(port).build()

  redis.start()

  override def afterAll(): Unit = {
    Await.result(redisClient.shutdownAsync().toCompletableFuture.toScala, 5.seconds)
    Await.result(actorSystem.terminate(), 5.seconds)
    redis.stop()
  }

  test("pub-sub") {
    val prefix    = Prefix("test.prefix")
    val eventName = EventName("one")
    val event1    = SystemEvent(prefix, eventName)
    val event2    = SystemEvent(prefix, eventName)
    val event3    = SystemEvent(prefix, eventName)
    val eventKey  = event1.eventKey

    val (killSwitch, seqF) = subscriberDriver.subscribe(Seq(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    publisherDriver.publish(eventKey, event1).await
    publisherDriver.publish(eventKey, event2).await

    subscriberDriver.unsubscribe(Seq(eventKey)).await

    publisherDriver.publish(eventKey, event3).await

    killSwitch.shutdown()

    seqF.await.map(_.value) shouldBe Seq(event1, event2)
  }

}
