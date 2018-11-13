package csw.config.client.commons
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}
import csw.config.api.exceptions.{LocalFileNotFound, UnableToParseOptions}
import csw.config.api.models.ConfigData
import csw.config.api.scaladsl.ConfigClientService
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class ConfigUtilsTest extends FunSuite with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with MockitoSugar {

  implicit val system: ActorSystem = ActorSystem("test")
  implicit val mat: Materializer   = ActorMaterializer()

  test("should throw exception if input file and default config is empty") {
    val mockedConfigClientService = mock[ConfigClientService]
    val configUtils               = new ConfigUtils(mockedConfigClientService)(system, mat)

    val exception = intercept[UnableToParseOptions.type] {
      Await.result(configUtils.getConfig(isLocal = false, inputFilePath = None, defaultConfig = None), 7.seconds)
    }

    exception.getMessage shouldEqual "Could not parse command line options. See --help to know more."
  }

  test("should return default config if input file if not provided") {
    val mockedConfigClientService = mock[ConfigClientService]
    val configUtils               = new ConfigUtils(mockedConfigClientService)(system, mat)
    val testConfig: Config        = system.settings.config

    val actualConfig =
      Await.result(configUtils.getConfig(isLocal = false, inputFilePath = None, defaultConfig = Some(testConfig)), 7.seconds)

    actualConfig shouldEqual testConfig
  }

  test("should use input file for config") {
    val mockedConfigClientService = mock[ConfigClientService]
    val configUtils               = new ConfigUtils(mockedConfigClientService)(system, mat)
    val filePath                  = Paths.get(getClass.getResource("/application.conf").getPath)
    val expectedConfig            = ConfigFactory.parseFile(filePath.toFile)

    val actualConfig =
      Await.result(configUtils.getConfig(isLocal = true, inputFilePath = Some(filePath), defaultConfig = None), 7.seconds)

    actualConfig shouldEqual expectedConfig
  }

  test("should throw exception if input file does not exist") {
    val mockedConfigClientService = mock[ConfigClientService]
    val configUtils               = new ConfigUtils(mockedConfigClientService)(system, mat)
    val invalidFilePath           = Paths.get("/invalidPath.conf")

    val exception = intercept[LocalFileNotFound] {
      Await.result(configUtils.getConfig(isLocal = true, inputFilePath = Some(invalidFilePath), defaultConfig = None), 7.seconds)
    }

    exception.getMessage shouldEqual s"File does not exist on local disk at path ${invalidFilePath.toString}"
  }

  test("should get config from remote input file") {
    val mockedConfigClientService = mock[ConfigClientService]
    val configUtils               = new ConfigUtils(mockedConfigClientService)(system, mat)
    val remoteFilePath            = Paths.get("remoteFile.conf")
    val configValue1: String =
      """
        |axisName1 = tromboneAxis1
        |axisName2 = tromboneAxis2
        |axisName3 = tromboneAxis3
        |""".stripMargin
    val expectedConfigData = ConfigData.fromString(configValue1)
    val expectedConfig     = Await.result(expectedConfigData.toConfigObject, 7.seconds)

    when(mockedConfigClientService.getActive(remoteFilePath))
      .thenReturn(Future.successful(Some(expectedConfigData)))

    val actualConfig =
      Await.result(configUtils.getConfig(isLocal = false, inputFilePath = Some(remoteFilePath), defaultConfig = None), 7.seconds)

    actualConfig shouldEqual expectedConfig
  }
}
