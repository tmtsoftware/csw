package csw.services.event.internal.redis

import java.io.IOException
import java.net.ServerSocket

import com.github.sebruck.EmbeddedRedis
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
import csw.services.event.internal.Wiring
import csw_protobuf.events.PbEvent
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.annotation.tailrec
import scala.compat.java8.FutureConverters.CompletionStageOps
import scala.concurrent.duration.{Duration, DurationDouble}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class RedisEventBusDriverTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {

  private val wiring = new Wiring()
  import wiring._
  lazy val redis: RedisServer = RedisServer.builder().setting("bind 127.0.0.1").port(redisPort).build()

  redis.start()

  val prefix             = Prefix("test.prefix")
  val eventName          = EventName("system")
  val event              = SystemEvent(prefix, eventName)
  val pbEvent: PbEvent   = Event.typeMapper.toBase(event)
  val eventKey: EventKey = event.eventKey

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
    Await.result(redisClient.shutdownAsync().toCompletableFuture.toScala, 5.seconds)
    Await.result(actorSystem.terminate(), 5.seconds)
    redis.stop()
  }

  test("abc") {
    println()
    eventBusDriver.subscribe("abc").runForeach(println)
    eventBusDriver.publish("abc", pbEvent).await
    eventBusDriver.unsubscribe(Seq("abc")).await
    Thread.sleep(1000)
    eventBusDriver.publish("abc", pbEvent).await
    Thread.sleep(1000)
  }

  implicit class RichFuture[T](f: Future[T]) {
    def await(duration: Duration): T = Await.result(f, duration)
    def await: T                     = await(Duration.Inf)
  }

}
