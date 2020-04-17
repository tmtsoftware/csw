package csw.command.client.models

import com.typesafe.config.ConfigFactory
import csw.prefix.models.Subsystem.{IRIS, TCS}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CommandRolesTest extends AnyFunSuite with Matchers {
  private val testConfig = ConfigFactory.parseString("""
                                           |setVoltage: [IRIS-user, APS-eng]
                                           |setFirmware: [TCS-admin]
                                           |setLimits: [WFOS-eng, TCS-eng]
                                           |""".stripMargin)

  test("CommandRoles.from should successfully parse config file and store it in lower case") {
    val expectedRoles = CommandRoles(
      Map(
        "setvoltage"  -> Set("iris-user", "aps-eng"),
        "setfirmware" -> Set("tcs-admin"),
        "setlimits"   -> Set("wfos-eng", "tcs-eng")
      )
    )
    val actualRoles = CommandRoles.from(testConfig)
    actualRoles shouldBe expectedRoles
  }

  test("CommandRoles.hasAccess should return true when user has correct role") {
    val roles = CommandRoles.from(testConfig)
    roles.hasAccess("setVoltage", IRIS, Set("IRIS-user")) shouldBe true
    roles.hasAccess("setVoltage", IRIS, Set("APS-eng")) shouldBe true
    roles.hasAccess("setVoltage", IRIS, Set("IRIS-user", "APS-eng")) shouldBe true
  }

  test("CommandRoles.hasAccess should return false when user does not have required role") {
    val roles = CommandRoles.from(testConfig)
    roles.hasAccess("setVoltage", IRIS, Set("WFOS-user")) shouldBe false
    roles.hasAccess("setFirmware", TCS, Set("TCS-eng")) shouldBe false
  }

  test(
    "CommandRoles.hasAccess should return true when command name does not exist but has user level access to destination subsystem"
  ) {
    val roles = CommandRoles.from(testConfig)
    roles.hasAccess("setExposure", IRIS, Set("IRIS-user")) shouldBe true
  }

  test(
    "CommandRoles.hasAccess should return false when command name does not exist and does not have user level access to destination subsystem"
  ) {
    val roles = CommandRoles.from(testConfig)
    roles.hasAccess("setExposure", IRIS, Set("WFOS-user")) shouldBe false
  }
}
