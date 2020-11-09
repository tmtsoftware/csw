package csw.location.agent.args

import csw.location.api.models.NetworkType
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class OptionsTest extends AnyFunSuiteLike with Matchers {

  test("should set networkType to NetworkType.Private by default when publicNetwork option not given | CSW-96") {
    Options().networkType shouldBe NetworkType.Inside
  }

  test("should set networkType to NetworkType.Public when publicNetwork is true | CSW-96") {
    Options(publicNetwork = true).networkType shouldBe NetworkType.Outside
  }

}
