package csw.location.agent.args

import csw.location.api.models.NetworkType
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class OptionsTest extends AnyFunSuiteLike with Matchers {

  test("should set networkType to NetworkType.Inside by default when outsideNetwork option not given | CSW-96") {
    Options().networkType shouldBe NetworkType.Inside
  }

  test("should set networkType to NetworkType.Outside when outsideNetwork is true | CSW-96") {
    Options(outsideNetwork = true).networkType shouldBe NetworkType.Outside
  }

}
