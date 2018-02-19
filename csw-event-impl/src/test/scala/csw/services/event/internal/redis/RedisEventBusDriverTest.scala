package csw.services.event.internal.redis

import akka.stream.scaladsl.{Keep, Sink}
import com.github.sebruck.EmbeddedRedis
import csw.services.event.helpers.PortHelper
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.Wiring
import csw_protobuf.events.PbEvent
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class RedisEventBusDriverTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {

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
    val key                = "abc"
    val (killSwitch, seqF) = eventBusDriver.subscribe(Seq(key)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)
    eventBusDriver.publish(key, PbEvent().withEventId("1")).await
    eventBusDriver.publish(key, PbEvent().withEventId("2")).await
    eventBusDriver.unsubscribe(Seq(key)).await
    eventBusDriver.publish(key, PbEvent().withEventId("3")).await
    killSwitch.shutdown()
    seqF.await.map(_.value) shouldBe Seq(PbEvent().withEventId("1"), PbEvent().withEventId("2"))
  }

}
