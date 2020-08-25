package csw.command.client.auth

import com.typesafe.config.ConfigFactory
import csw.aas.http.Roles
import csw.prefix.models.Subsystem.{IRIS, TCS}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CommandRolesTest extends AnyFunSuite with Matchers {
  private val testConfig =
    ConfigFactory.parseString("""
                                           |IRIS.instrument1.setTemperature: [APS-eng]
                                           |IRIS.instrument1.setVoltage: [IRIS-eng, APS-eng]
                                           |TCS.filter.wheel.setFirmware: [TCS-admin]
                                           |WFOS.mirror.setLimits: [WFOS-eng, TCS-eng]
                                           |""".stripMargin)

  private val setTemperatureCmdKey = CommandKey("iris.instrument1.settemperature")
  private val setVoltageCmdKey     = CommandKey("iris.instrument1.setvoltage")
  private val setFirmwareCmdKey    = CommandKey("tcs.filter.wheel.setfirmware")
  private val setLimitsCmdKey      = CommandKey("wfos.mirror.setlimits")

  test("CommandRoles.from should successfully parse config file and store it in lower case in the appropriate models") {

    val expectedMapping = Map(
      setTemperatureCmdKey -> Roles(Set("aps-eng")),
      setVoltageCmdKey     -> Roles(Set("iris-eng", "aps-eng")),
      setFirmwareCmdKey    -> Roles(Set("tcs-admin")),
      setLimitsCmdKey      -> Roles(Set("wfos-eng", "tcs-eng"))
    )

    val actualRoles = CommandRoles.from(testConfig)
    actualRoles.predefinedRoles shouldBe expectedMapping
  }

  test("CommandRoles.hasAccess should return true when user has correct role | ESW-95") {
    val roles = CommandRoles.from(testConfig)
    roles.hasAccess(setVoltageCmdKey, IRIS, Roles(Set("IRIS-admin", "IRIS-eng"))) shouldBe true
    roles.hasAccess(setVoltageCmdKey, IRIS, Roles(Set("IRIS-eng"))) shouldBe true
    roles.hasAccess(setVoltageCmdKey, IRIS, Roles(Set("IRIS-user"))) shouldBe false
    roles.hasAccess(setVoltageCmdKey, IRIS, Roles(Set("APS-eng"))) shouldBe true
    roles.hasAccess(setVoltageCmdKey, IRIS, Roles(Set("IRIS-eng", "APS-eng"))) shouldBe true
    roles.hasAccess(setTemperatureCmdKey, IRIS, Roles(Set("APS-eng"))) shouldBe true
    roles.hasAccess(setTemperatureCmdKey, IRIS, Roles(Set("IRIS-user"))) shouldBe true
  }

  test("CommandRoles.hasAccess should return false when user does not have required role | ESW-95") {
    val roles = CommandRoles.from(testConfig)
    roles.hasAccess(setVoltageCmdKey, IRIS, Roles(Set("IRIS-user"))) shouldBe false
    roles.hasAccess(setVoltageCmdKey, IRIS, Roles(Set("WFOS-user"))) shouldBe false
    roles.hasAccess(setFirmwareCmdKey, TCS, Roles(Set("TCS-eng"))) shouldBe false
  }

  test(
    "CommandRoles.hasAccess should return true when command name does not exist but has user level access to destination subsystem | ESW-95"
  ) {
    val roles = CommandRoles.from(testConfig)
    roles.hasAccess(CommandKey("IRIS.filter.setExposure"), IRIS, Roles(Set("IRIS-user"))) shouldBe true
  }

  test(
    "CommandRoles.hasAccess should return false when command name does not exist and does not have user level access to destination subsystem | ESW-95"
  ) {
    val roles = CommandRoles.from(testConfig)
    roles.hasAccess(CommandKey("IRIS.filter.setExposure"), IRIS, Roles(Set("WFOS-user"))) shouldBe false
  }
}
