package csw.params.core.models

import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.{FunSuite, Matchers}

class PrefixTest extends FunSuite with Matchers {

  test("should able to create Prefix and access subsystem from valid prefix string") {
    val prefixStr = "tcs.mobie.blue.filter"
    val prefix    = Prefix(prefixStr)

    prefix.subsystem shouldBe Subsystem.TCS
  }

  test("should now allow creating Prefix when invalid prefix string provided") {
    val prefixStr = "invalid.prefix"
    a[NoSuchElementException] shouldBe thrownBy(Prefix(prefixStr))
  }
}
