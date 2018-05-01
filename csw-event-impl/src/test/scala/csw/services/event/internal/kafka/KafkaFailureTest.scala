package csw.services.event.internal.kafka

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.scaladsl.Source
import akka.testkit.typed.scaladsl.TestProbe
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.exceptions.PublishFailed
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.Utils
import csw.services.event.internal.wiring.FailedEvent
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationInt

//DEOPSCSW-398: Propagate failure for publish api (eventGenerator)
class KafkaFailureTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  val kafkaTestProps: KafkaTestProps = KafkaTestProps.createKafkaProperties(3559, 6001, Map("message.max.bytes" → "1"))
  import kafkaTestProps._
  import kafkaTestProps.wiring._

  override def beforeAll(): Unit = {
    EmbeddedKafka.start()(config)
  }

  override def afterAll(): Unit = {
    publisher.shutdown().await
    EmbeddedKafka.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  test("failure in publishing should fail future with PublishFailed exception") {

    // simulate publishing failure as message size is greater than message.max.bytes(1 byte) configured in broker
    intercept[PublishFailed] {
      publisher.publish(Utils.makeEvent(2)).await
    }
  }

  //DEOPSCSW-334: Publish an event
  test("handle failed publish event with a callback") {

    val testProbe   = TestProbe[FailedEvent]()(actorSystem.toTyped)
    val event       = Utils.makeEvent(1)
    val eventStream = Source.single(event)

    publisher.publish(eventStream, (event, ex) ⇒ testProbe.ref ! FailedEvent(event, ex))

    val failedEvent = testProbe.expectMessageType[FailedEvent]

    failedEvent.event shouldBe event
    failedEvent.throwable shouldBe a[PublishFailed]
  }

  //DEOPSCSW-334: Publish an event
  test("handle failed publish event with an eventGenerator and a callback") {
    val testProbe = TestProbe[FailedEvent]()(actorSystem.toTyped)
    val event     = Utils.makeEvent(1)

    publisher.publish(event, 20.millis, (event, ex) ⇒ testProbe.ref ! FailedEvent(event, ex))

    val failedEvent = testProbe.expectMessageType[FailedEvent]

    failedEvent.event shouldBe event
    failedEvent.throwable shouldBe a[PublishFailed]
  }
}
