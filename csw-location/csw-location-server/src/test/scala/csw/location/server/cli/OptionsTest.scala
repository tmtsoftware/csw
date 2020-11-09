package csw.location.server.cli

import csw.location.api.models.NetworkType
import csw.network.utils.Networks
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers

class OptionsTest extends AnyFunSuiteLike with Matchers {

  test("should set httpBindHost to 127.0.0.1 by default when outsideNetwork option not given | CSW-96, CSW-89") {
    Options().httpBindHost shouldBe "127.0.0.1"
  }

  test("should set httpBindHost to Public network IP when outsideNetwork is true | CSW-96, CSW-89") {
    Options(outsideNetwork = true).httpBindHost shouldBe Networks(NetworkType.Outside.envKey).hostname
  }
}
