package csw.params.core.models

import java.time.Year

import enumeratum.{EnumEntry, Enum}

import scala.collection.immutable.IndexedSeq

sealed trait Semester extends EnumEntry

object Semester extends Enum[Semester] {
  override def values: IndexedSeq[Semester] = findValues

  case object A extends Semester
  case object B extends Semester
}

case class SemesterId private (year: Year, semester: Semester) {
  override def toString: String = s"$year$semester"
}

object SemesterId {
  def apply(semesterId: String): SemesterId = {
    val (yearStr, semesterStr) = semesterId.splitAt(semesterId.length - 1)
    require(yearStr.toIntOption.isDefined, s"$yearStr should be valid year")
    SemesterId(Year.of(yearStr.toInt), Semester.withNameInsensitive(semesterStr))
  }
}
