package csw.location.agent.utils

import java.io.File

import com.typesafe.config.Config
import csw.commons.ResourceReader
import org.scalatest.{FunSuite, Matchers}

class UtilsTest extends FunSuite with Matchers {

  test("testGetAppConfig") {
    val configFile        = ResourceReader.copyToTmp("/redisTest.conf").toFile
    val x: Option[Config] = Utils.getAppConfig(configFile)

    x.isDefined shouldBe true
    x.get.getString("csw.redistest.port") shouldBe "7777"
  }

  test("testNonExistantAppConfig") {
    val configFile        = new File("/doesNotExist.conf")
    val x: Option[Config] = Utils.getAppConfig(configFile)
    x shouldBe None
  }
}
