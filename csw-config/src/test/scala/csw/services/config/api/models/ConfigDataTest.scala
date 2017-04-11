package csw.services.config.api.models

import java.io.InputStream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.testkit.TestSubscriber.Probe
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import akka.util.ByteString
import csw.services.config.api.commons.TestFutureExtension.RichFuture
import org.scalatest.{FunSuiteLike, Matchers}

class ConfigDataTest extends TestKit(ActorSystem("test-system")) with FunSuiteLike with Matchers {

  implicit val mat: Materializer = ActorMaterializer()

  val expectedStringConfigData =
    """
      |This is a string for testing.
      |Which should be converted to ByteString first.
      |Then create a source from ByteString
      |""".stripMargin

  private val sourceOfByteString: Source[ByteString, NotUsed] = Source.single(ByteString(expectedStringConfigData.getBytes))

  test("should able to retrieve string from Config Data source") {
    val actualStringConfigData = new ConfigData(sourceOfByteString).toStringF.await
    actualStringConfigData shouldEqual expectedStringConfigData
  }

  test("should able to generate InputStream from Config Data source") {
    val configDataInputStream: InputStream = new ConfigData(sourceOfByteString).toInputStream
    scala.io.Source.fromInputStream(configDataInputStream).mkString shouldEqual expectedStringConfigData
  }

  test("should create source of ByteString from string") {
    val fromString: ConfigData = ConfigData.fromString(expectedStringConfigData)
    val probe: Probe[ByteString] = fromString.source.runWith(TestSink.probe)

    probe.requestNext().utf8String shouldEqual expectedStringConfigData
  }

  test("should create Config data of source of ByteString from ByteString source") {
    val fromSource: ConfigData = ConfigData.fromSource(sourceOfByteString)

    fromSource.source shouldEqual sourceOfByteString
  }

}
