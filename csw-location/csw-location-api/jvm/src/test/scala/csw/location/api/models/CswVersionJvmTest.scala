package csw.location.api.models

import csw.location.api.CswVersionJvm
import csw.location.api.client.CswVersion
import csw.prefix.models.Prefix
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CswVersionJvmTest extends AnyFunSuite with Matchers {
  private val cswVersion: CswVersion = new CswVersionJvm
  private val prefix                 = Prefix("CSW.Version")

  test("it should return true when current version equals to given metadata's csw version | CSW-90") {
    cswVersion.check(Metadata.empty.withCSWVersion("0.1.0-SNAPSHOT"), prefix) shouldBe true
  }

  test("it should return false when current version isn't equals to given metadata's csw version | CSW-90") {
    cswVersion.check(Metadata.empty.withCSWVersion("1.5.6"), prefix) shouldBe false
  }
}
