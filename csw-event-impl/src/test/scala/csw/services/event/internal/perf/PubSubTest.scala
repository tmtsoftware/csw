package csw.services.event.internal.perf

import akka.stream.scaladsl.{Sink, Source}
import csw.messages.ccs.events.EventKey
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils._
import csw.services.event.internal.Wiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class PubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedKafka {

  private val wiring = new Wiring()

  import wiring._
  lazy val redis: RedisServer = RedisServer.builder().setting("bind 127.0.0.1").port(6379).build()

  override protected def beforeAll(): Unit = {
    redis.start()
    EmbeddedKafka.start()
  }

  override def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
    kafkaPublisher.shutdown().await
    kafkaSubscriber.shutdown().await
    EmbeddedKafka.stop()
    Await.result(redisGateway.shutdown(), 5.seconds)
    redis.stop()
  }

  val eventKey: EventKey = makeEvent(0).eventKey

  var counter = 0
  private def eventGenerator() = {
    counter += 1
    makeEvent(counter)
  }
  private val eventStream = Source.repeat(()).map(_ => eventGenerator())

  ignore("redis-throughput-latency") {
    runPerf(redisPublisher, redisSubscriber)
  }

  ignore("kafka-throughput-latency") {
    runPerf(kafkaPublisher, kafkaSubscriber)
  }

  def runPerf(publisher: EventPublisher, subscriber: EventSubscriber): Unit = {
    val doneF = subscriber
      .subscribe(Set(eventKey), 20.millis)
      .via(Monitor.resetting)
      .runWith(Sink.ignore)

    Thread.sleep(1000)

//    eventStream.mapAsync(1)(publisher.publish).runWith(Sink.ignore)
//    publisher.publish(eventStream)
    publisher.publish(eventGenerator, 5.millis)

    doneF.await
  }
}
