package csw.services.tracklocation.utils

import java.io.File
import java.nio.file.Paths

import com.typesafe.config.Config
import csw.services.tracklocation.models.Options
import org.scalatest.{FunSuite, Matchers}

class UtilsTest extends FunSuite with Matchers {

  test("testGetFreePort") {
    Utils.getFreePort should not be 0
  }

  test("testGetAppConfig") {
    val url            = getClass.getResource("/redisTest.conf")
    val configFilePath = Paths.get(url.toURI).toFile.getAbsolutePath
    val configFile     = new File(configFilePath)

    val x: Option[Config] = Utils.getAppConfig(configFile)

    x.isDefined shouldBe true
    x.get.getString("redisTest.port") shouldBe "7777"
  }
}
