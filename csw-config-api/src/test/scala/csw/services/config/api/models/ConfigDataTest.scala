package csw.services.config.api.models

import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import csw.services.config.api.commons.TestFutureExtension.RichFuture
import org.scalatest.{FunSuiteLike, Matchers}

class ConfigDataTest extends TestKit(ActorSystem("test-system")) with FunSuiteLike with Matchers {

  implicit val mat: Materializer = ActorMaterializer()

  val expectedStringConfigData: String =
    """
      |This is a string for testing.
      |Which should be converted to ByteString first.
      |Then create a source from ByteString
      |""".stripMargin

  test("should able to retrieve string from Config Data source") {
    ConfigData.fromString(expectedStringConfigData).toStringF.await shouldEqual expectedStringConfigData
  }

  test("should able to generate InputStream from Config Data source") {
    val inputStream = ConfigData.fromString(expectedStringConfigData).toInputStream
    scala.io.Source.fromInputStream(inputStream).mkString shouldEqual expectedStringConfigData
  }

  test("should create source of ByteString from string") {
    val configData = ConfigData.fromString(expectedStringConfigData)
    configData.source.runFold("")(_ + _.utf8String).await shouldEqual expectedStringConfigData
  }

  //DEOPSCSW-72: Retrieve a configuration file to a specified file location on a local disk
  test("should be able to save ConfigData to local disc") {
    val configData     = ConfigData.fromString(expectedStringConfigData)
    val tempOutputFile = Files.createTempFile("temp-config", ".conf")
    configData.toPath(tempOutputFile).await
    new String(Files.readAllBytes(tempOutputFile)) shouldBe expectedStringConfigData
    Files.delete(tempOutputFile)
  }
}
