package csw.prefix.models

import csw.prefix.models.Subsystem.TCS
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class PrefixTest extends AnyFunSuite with Matchers {

  test("should able to create Prefix and access subsystem from valid prefix string") {
    val prefixStr = "tcs.mobie.blue.filter"
    val prefix    = Prefix(prefixStr)

    prefix.subsystem shouldBe Subsystem.TCS
  }

  test("should now allow creating Prefix when invalid prefix string provided") {
    val prefixStr = "invalid.prefix"
    a[NoSuchElementException] shouldBe thrownBy(Prefix(prefixStr))
  }

  // CSW-86: Subsystem should be case-insensitive
  test("should access subsystem and componentName in lowercase") {
    val prefix = Prefix("Tcs.Filter.Wheel")
    prefix.toString shouldEqual "TCS.Filter.Wheel"
    prefix.subsystem shouldBe TCS
    prefix.componentName shouldBe "Filter.Wheel"
  }
}
