package csw.services.event.internal.perf

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.{Id, Prefix}
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.Wiring
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

import scala.compat.java8.FutureConverters.CompletionStageOps
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
    EmbeddedKafka.stop()
    Await.result(redisClient.shutdownAsync().toCompletableFuture.toScala, 5.seconds)
    redis.stop()
  }

  val prefix                   = Prefix("test.prefix")
  val eventName                = EventName("system")
  def makeEvent(x: Int): Event = SystemEvent(prefix, eventName).copy(eventId = Id(x.toString))
  val eventKey: EventKey       = makeEvent(0).eventKey

  ignore("redis-throughput-latency") {
    perf(redisPublisher, redisSubscriber)
  }

  ignore("kafka-throughput-latency") {
    perf(kafkaPublisher, kafkaSubscriber)
  }

  private val buckets = List(3, 5, 7, 9)

  private val monitor = Flow[Event].statefulMapConcat { () =>
    var last      = 0
    var count     = 0
    val startTime = System.currentTimeMillis()
    val latencies = collection.mutable.Map.empty[Long, Long].withDefaultValue(0)

    x =>
      val eventTime   = x.eventTime.time.toEpochMilli
      val currentTime = System.currentTimeMillis
      val latency     = currentTime - eventTime
      val accTime     = currentTime - startTime

      count += 1
      val throughput = (count * 1000) / accTime

      val currentId    = x.eventId.id.toInt
      val isOutOfOrder = (currentId - last) < 1
      last = currentId

      buckets.foreach { x =>
        if (latency <= x) {
          latencies(x) += 1
        }
      }

      val latencyStr = latencies.toList.sortBy(_._1).map {
        case (k, v) =>
          val percentile = v * 100.0 / count
          s"${k}ms -> ${f"$percentile%2.2f"}"
      }

      if (isOutOfOrder) {
        println(s"event_id=$currentId   throughput=$throughput    latency=$latencyStr   out of order!!!!")
      } else if (currentId % 1000 == 0) {
        println(s"event_id=$currentId   throughput=$throughput    latency=$latencyStr")
      }

      List(x)
  }

  def perf(publisher: EventPublisher, subscriber: EventSubscriber): Unit = {
    val (sub, seqF) = subscriber.subscribe(Set(eventKey)).via(monitor).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)

    var counter = 0
    def eventGenerator() = {
      counter += 1
      makeEvent(counter)
    }

    val eventStream = Source.fromIterator(() => Iterator.from(1)).map(makeEvent).watchTermination()(Keep.right)

//    publisher.publish(eventStream).await

//    eventStream.mapAsync(1)(publisher.publish).runWith(Sink.ignore).await

    val cancellable = publisher.publish(eventGenerator, 1.millis)
    Thread.sleep(100000)
    cancellable.cancel()
  }
}
