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

case class SemesterId(year: Year, semester: Semester) {
  override def toString: String = s"$year$semester"
}

object SemesterId {
  def apply(semesterId: String): SemesterId = {
    val (yearStr, semesterStr) = semesterId.splitAt(semesterId.length - 1)
    SemesterId(Year.parse(yearStr), Semester.withNameInsensitive(semesterStr))
  }
}
