package csw.services.config.models

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import akka.util.ByteString
import csw.services.config.api.models.ConfigData
import csw.services.config.commons.TestFutureExtension.RichFuture
import org.scalatest.{FunSuiteLike, Matchers}

class ConfigDataTest extends TestKit(ActorSystem("test-system")) with FunSuiteLike with Matchers {

  implicit val mat: Materializer = ActorMaterializer()

  val expectedStringConfigData: String =
    """
      |This is a string for testing.
      |Which should be converted to ByteString first.
      |Then create a source from ByteString
      |""".stripMargin

  private val sourceOfByteString = Source.single(ByteString(expectedStringConfigData))

  test("should able to retrieve string from Config Data source") {
    new ConfigData(sourceOfByteString).toStringF.await shouldEqual expectedStringConfigData
  }

  test("should able to generate InputStream from Config Data source") {
    val inputStream = new ConfigData(sourceOfByteString).toInputStream
    scala.io.Source.fromInputStream(inputStream).mkString shouldEqual expectedStringConfigData
  }

  test("should create source of ByteString from string") {
    val configData = ConfigData.fromString(expectedStringConfigData)
    configData.source.runFold("")(_ + _.utf8String).await shouldEqual expectedStringConfigData
  }
}
