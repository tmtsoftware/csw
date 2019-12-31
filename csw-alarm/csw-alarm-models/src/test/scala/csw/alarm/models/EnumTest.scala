package csw.alarm.models

import enumeratum.{Enum, EnumEntry}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

abstract class EnumTest[T <: EnumEntry](enum: Enum[T]) extends AnyFunSuite with Matchers {
  val expectedValues: Set[T]
  val actualValues: Set[T] = enum.values.toSet

  test(s"should match enum values") {
    actualValues shouldEqual expectedValues
  }
}
