package csw.event.cli.args

import csw.params.core.models.Units.{NoUnits, volt}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-436: [Event Cli] Specialized Publish command (take params from command line)
class KeyArgTest extends AnyFunSuite with Matchers {

  test("should able to parse valid ':' separated string when key name, type and units provided | DEOPSCSW-436") {
    val arg = KeyArg("name:s:volt")
    arg.keyName shouldBe "name"
    arg.keyType shouldBe 's'
    arg.units shouldBe volt
  }

  test("should able to parse valid ':' separated string when units are not provided | DEOPSCSW-436") {
    val arg = KeyArg("name:s")

    arg.keyName shouldBe "name"
    arg.keyType shouldBe 's'
    arg.units shouldBe NoUnits
  }

  test("should throw exception when key type is missing | DEOPSCSW-436") {
    intercept[RuntimeException](KeyArg("name"))
  }

}
