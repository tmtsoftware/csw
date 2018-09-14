package csw.services.event.cli.args

import csw.params.core.models.Units.{volt, NoUnits}
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-436: [Event Cli] Specialized Publish command (take params from command line)
class KeyArgTest extends FunSuite with Matchers {

  test("should able to parse valid ':' separated string when key name, type and units provided") {
    KeyArg("name:s:volt") shouldBe KeyArg("name", 's', volt)
  }

  test("should able to parse valid ':' separated string when units are not provided") {
    KeyArg("name:s") shouldBe KeyArg("name", 's', NoUnits)
  }

  test("should throw exception when key type is missing") {
    intercept[RuntimeException](KeyArg("name"))
  }

}
