package csw.event.client.internal.kafka

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.stream.scaladsl.Source
import csw.event.api.exceptions.PublishFailure
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.event.client.helpers.Utils
import csw.params.events.Event
import csw.time.core.models.UTCTime
import org.apache.kafka.common.errors.RecordTooLargeException
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
class KafkaFailureTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  val kafkaTestProps: KafkaTestProps =
    KafkaTestProps.createKafkaProperties(additionalBrokerProps = Map("message.max.bytes" → "1"))
  import kafkaTestProps._

  override def beforeAll(): Unit = {
    kafkaTestProps.start()
  }

  override def afterAll(): Unit = kafkaTestProps.shutdown()

  test("failure in publishing should fail future with PublishFailed exception") {

    // simulate publishing failure as message size is greater than message.max.bytes(1 byte) configured in broker
    val failedEvent = Utils.makeEvent(2)
    val failure = intercept[PublishFailure] {
      publisher.publish(failedEvent).await
    }
    failure.event shouldBe failedEvent
    failure.getCause shouldBe a[RecordTooLargeException]

  }

  //DEOPSCSW-334: Publish an event
  test("handle failed publish event with a callback") {

    val testProbe   = TestProbe[PublishFailure]()(typedActorSystem)
    val failedEvent = Utils.makeEvent(1)
    val eventStream = Source.single(failedEvent)

    publisher.publish(eventStream, onError = testProbe.ref ! _)

    val failure = testProbe.expectMessageType[PublishFailure]

    failure.event shouldBe failedEvent
    failure.getCause shouldBe a[RecordTooLargeException]
  }

  //DEOPSCSW-334: Publish an event
  test("handle failed publish event with an eventGenerator and a callback") {
    val testProbe   = TestProbe[PublishFailure]()(typedActorSystem)
    val failedEvent = Utils.makeEvent(1)

    publisher.publish(Some(failedEvent), 20.millis, onError = testProbe.ref ! _)

    val failure = testProbe.expectMessageType[PublishFailure]

    failure.event shouldBe failedEvent
    failure.getCause shouldBe a[RecordTooLargeException]
  }

  //DEOPSCSW-000: Publish events with block generating futre of event
  test("handle failed publish event with an eventGenerator generating future of event and a callback") {
    val testProbe   = TestProbe[PublishFailure]()(typedActorSystem)
    val failedEvent = Utils.makeEvent(1)

    publisher.publishAsync(Future.successful(Some(failedEvent)), 20.millis, onError = testProbe.ref ! _)

    val failure = testProbe.expectMessageType[PublishFailure]

    failure.event shouldBe failedEvent
    failure.getCause shouldBe a[RecordTooLargeException]
  }

  //DEOPSCSW-515: Include Start Time in API
  test("should invoke onError callback on publish failure [eventGenerator API] with start time and event generator") {
    val testProbe   = TestProbe[PublishFailure]()(typedActorSystem)
    val failedEvent = Utils.makeEvent(1)

    def eventGenerator(): Some[Event] = Some(failedEvent)

    val startTime = UTCTime(UTCTime.now().value.plusMillis(500))

    publisher.publish(eventGenerator(), startTime, 20.millis, onError = testProbe.ref ! _)

    val failure = testProbe.expectMessageType[PublishFailure]

    failure.event shouldBe failedEvent
    failure.getCause shouldBe a[RecordTooLargeException]
  }

  //DEOPSCSW-515: Include Start Time in API
  test("should invoke onError callback on publish failure [eventGenerator API] with start time and future of event generator") {
    val testProbe   = TestProbe[PublishFailure]()(typedActorSystem)
    val failedEvent = Utils.makeEvent(1)

    def eventGenerator(): Future[Option[Event]] = Future.successful(Some(failedEvent))

    val startTime = UTCTime(UTCTime.now().value.plusMillis(500))

    publisher.publishAsync(eventGenerator(), startTime, 20.millis, onError = testProbe.ref ! _)

    val failure = testProbe.expectMessageType[PublishFailure]

    failure.event shouldBe failedEvent
    failure.getCause shouldBe a[RecordTooLargeException]
  }

  //DEOPSCSW-516: Optionally Publish - API Change
  test("should not invoke onError on opting to not publish event with eventGenerator") {
    val testProbe = TestProbe[PublishFailure]()(typedActorSystem)

    publisher.publish(None, 20.millis, onError = testProbe.ref ! _)
    testProbe.expectNoMessage

    val startTime = UTCTime(UTCTime.now().value.plusMillis(200))
    publisher.publish(None, startTime, 20.millis, onError = testProbe.ref ! _)
    testProbe.expectNoMessage(500.millis)
  }

  //DEOPSCSW-516: Optionally Publish - API Change
  test("should not invoke onError on opting to not publish event with async eventGenerator") {
    val testProbe = TestProbe[PublishFailure]()(typedActorSystem)

    def eventGenerator(): Future[Option[Event]] = Future.successful(None)

    publisher.publishAsync(eventGenerator(), 20.millis, onError = testProbe.ref ! _)
    testProbe.expectNoMessage

    val startTime = UTCTime(UTCTime.now().value.plusMillis(200))
    publisher.publishAsync(eventGenerator(), startTime, 20.millis, onError = testProbe.ref ! _)
    testProbe.expectNoMessage(500.millis)
  }

}
