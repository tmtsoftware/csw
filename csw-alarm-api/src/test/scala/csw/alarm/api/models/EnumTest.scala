package csw.alarm.api.models

import enumeratum.{Enum, EnumEntry}
import org.scalatest.{FunSuite, Matchers}

abstract class EnumTest[T <: EnumEntry](enum: Enum[T]) extends FunSuite with Matchers {
  val expectedValues: Set[T]
  val actualValues: Set[T] = enum.values.toSet

  test(s"should match enum values") {
    actualValues shouldEqual expectedValues
  }
}
