package csw.config.api.commons

import akka.util.ByteString
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class BinaryUtilsTest extends AnyFunSuite with Matchers {
  test("testIsNotTextIsFalseForMoreThan85percentASCIICharacters") {
    val testString = """testingIsNotTextShøuldReturnFålse"""
    BinaryUtils.isNotText(testString.getBytes) shouldBe false
  }

  test("testIsBinaryIsFalseForMoreThan85percentASCIICharacters") {
    val testString = """testingIsBinΩryShouldReturnFålse"""
    BinaryUtils.isBinary(List(ByteString.fromString(testString))) shouldBe false
  }

  test("testIsNotTextIsTrueForLessThan85percentASCIICharacters") {
    val testString = """testingIsNøtTextShøuldRe∫urnTrπe"""
    BinaryUtils.isNotText(testString.getBytes) shouldBe true
  }

  test("testIsBinaryIsTrueForLessThan85percentASCIICharacters") {
    val testString = """testingIsBinΩr¥ShouldRe∫urnTrπe"""
    BinaryUtils.isBinary(List(ByteString.fromString(testString))) shouldBe true
  }
}
