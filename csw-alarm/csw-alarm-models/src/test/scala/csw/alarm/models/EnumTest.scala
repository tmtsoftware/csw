package csw.alarm.models

import enumeratum.{Enum, EnumEntry}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-441: Model to represent Alarm Acknowledgement status
abstract class EnumTest[T <: EnumEntry](enum: Enum[T], storyId: String) extends AnyFunSuite with Matchers {
  val expectedValues: Set[T]
  val actualValues: Set[T] = enum.values.toSet

  test(s"should match enum values $storyId") {
    actualValues shouldEqual expectedValues
  }
}
