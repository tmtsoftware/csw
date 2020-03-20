package csw.event.cli.args

import csw.params.core.models.Units.{volt, NoUnits}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-436: [Event Cli] Specialized Publish command (take params from command line)
class KeyArgTest extends AnyFunSuite with Matchers {

  test("should able to parse valid ':' separated string when key name, type and units provided | DEOPSCSW-436") {
    KeyArg("name:s:volt") shouldBe KeyArg("name", 's', volt)
  }

  test("should able to parse valid ':' separated string when units are not provided | DEOPSCSW-436") {
    KeyArg("name:s") shouldBe KeyArg("name", 's', NoUnits)
  }

  test("should throw exception when key type is missing | DEOPSCSW-436") {
    intercept[RuntimeException](KeyArg("name"))
  }

}
