package csw.services.event.internal.pubsub

import akka.stream.scaladsl.{Keep, Sink, Source}
import csw.messages.ccs.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.Prefix
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

  test("Redis pub-sub") {
    runPubSub(redisPublisher, redisSubscriber)
  }

  test("Redis independent-subscriptions") {
    runIndependentSubscriptions(redisPublisher, redisSubscriber)
  }

  ignore("Redis multiple-publish") {
    runMultiplePublish(redisPublisher, redisSubscriber)
  }

  ignore("Redis retrieve-recently-published-event-on-subscription") {
    runRetrieveRecentlyPublished(redisPublisher, redisSubscriber)
  }

  test("Kafka pub-sub") {
    runPubSub(kafkaPublisher, kafkaSubscriber)
  }

  test("Kafka independent-subscriptions") {
    runIndependentSubscriptions(redisPublisher, redisSubscriber)
  }

  ignore("Kafka multiple-publish") {
    runMultiplePublish(kafkaPublisher, kafkaSubscriber)
  }

  test("Kafka retrieve-recently-published-event-on-subscription") {
    runRetrieveRecentlyPublished(kafkaPublisher, kafkaSubscriber)
  }

  private def runPubSub(publisher: EventPublisher, subscriber: EventSubscriber) = {
    val event1             = makeEvent(1)
    val eventKey: EventKey = event1.eventKey

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(2000)

    publisher.publish(event1).await
    Thread.sleep(1000)

    subscription.unsubscribe().await
    seqF.await shouldBe List(event1)
  }

  private def runIndependentSubscriptions(publisher: EventPublisher, subscriber: EventSubscriber) = {

    val prefix        = Prefix("test.prefix")
    val eventName1    = EventName("system1")
    val eventName2    = EventName("system2")
    val event1: Event = SystemEvent(prefix, eventName1)
    val event2: Event = SystemEvent(prefix, eventName2)

    val (subscription, seqF) = subscriber.subscribe(Set(event1.eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)
    publisher.publish(event1).await

    val (subscription2, seqF2) = subscriber.subscribe(Set(event2.eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(1000)
    publisher.publish(event2).await

    subscription.unsubscribe().await
    subscription2.unsubscribe().await

    seqF.await shouldBe List(event1)
    seqF2.await shouldBe List(event2)
  }

  private def runMultiplePublish(publisher: EventPublisher, subscriber: EventSubscriber) = {
    def event: Event = makeEvent(1)

    val eventKey: EventKey = event.eventKey

    subscriber.subscribe(Set(eventKey)).runForeach { x =>
      val begin = x.eventTime.time.toEpochMilli
      println(System.currentTimeMillis() - begin)
    }

    Thread.sleep(10)

    publisher.publish(Source.fromIterator(() => Iterator.continually(event)).map(x => { println(s"from 1 -> $x"); x }))
    publisher
      .publish(
        Source
          .fromIterator(() => Iterator.continually(event))
          .map(x => { println(s"from 2            -> $x"); x })
          .watchTermination()(Keep.right)
      )
      .await
  }

  private def runRetrieveRecentlyPublished(publisher: EventPublisher, subscriber: EventSubscriber) = {
    val event1   = makeEvent(1)
    val event2   = makeEvent(2)
    val event3   = makeEvent(3)
    val eventKey = event1.eventKey

    publisher.publish(event1).await
    publisher.publish(event2).await

    val (subscription, seqF) = subscriber.subscribe(Set(eventKey)).toMat(Sink.seq)(Keep.both).run()
    Thread.sleep(2000)

    publisher.publish(event3).await
    Thread.sleep(1000)

    subscription.unsubscribe()

    seqF.await shouldBe Seq(event2, event3)
  }

}
