package csw.config.api.models

import java.nio.file.Files

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestKit
import com.typesafe.config.{Config, ConfigException}
import csw.config.api.commons.TestFutureExtension.RichFuture
import org.scalatest.{FunSuiteLike, Matchers}

import scala.collection.JavaConverters.asScalaBufferConverter

class ConfigDataTest extends TestKit(ActorSystem("test-system")) with FunSuiteLike with Matchers {

  implicit val mat: Materializer = ActorMaterializer()

  val expectedStringConfigData: String =
    """
      |This is a string for testing.
      |Which should be converted to ByteString first.
      |Then create a source from ByteString
      |""".stripMargin

  //DEOPSCSW-73: Retrieve a configuration file to memory
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
    Files.readString(tempOutputFile) shouldBe expectedStringConfigData
    Files.delete(tempOutputFile)
  }

  test("should be able to get Config object when data is in valid HOCON format") {
    val configStr = s"""
                     container {
                     |  name = Container-1
                     |  components {
                     |    Assembly-1 {
                     |      type = Assembly
                     |      class = csw.services.pkg.TestAssembly
                     |      prefix = tcs.base.assembly1
                     |      connectionType: [akka]
                     |      connections = [
                     |        // Component connections used by this component
                     |        {
                     |          name: HCD-2A
                     |          type: HCD
                     |          connectionType: [akka]
                     |        }
                     |        {
                     |          name: HCD-2B
                     |          type: HCD
                     |          connectionType: [akka]
                     |        }
                     |      ]
                     |    }
                     |  }
                     |}
       """.stripMargin

    val oConfig: Config = ConfigData.fromString(configStr).toConfigObject.await
    oConfig.getString("container.name") shouldBe "Container-1"
    oConfig
      .getConfigList("container.components.Assembly-1.connections")
      .asScala
      .map(_.getString("name"))
      .toSet shouldBe List("HCD-2A", "HCD-2B").toSet
  }

  test("config object conversion should receive exception when data is NOT in valid HOCON format") {
    val configStr = s"""
                     container {
                       |    }
                       |    ••¡•ººº¡¯˘ð´©ˍ
                       |  }
                       |}
       """.stripMargin

    intercept[ConfigException] {
      ConfigData.fromString(configStr).toConfigObject.await
    }
  }
}
