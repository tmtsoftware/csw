package csw.params.core.models

import java.time.Year

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

sealed trait Semester extends EnumEntry

object Semester extends Enum[Semester] {
  override def values: IndexedSeq[Semester] = findValues

  case object A extends Semester
  case object B extends Semester
}

/**
 * Represents a unique semester id
 *
 * @param year year for semester
 * @param semester observing semester
 */
case class SemesterId private[csw] (year: Year, semester: Semester) {
  override def toString: String = s"$year$semester"
}

object SemesterId {
  private def parseSemester(semesterStr: String): Semester =
    try {
      Semester.withNameInsensitive(semesterStr)
    }
    catch {
      case ex: Exception => throw new IllegalArgumentException(s"Failed to parse semester $semesterStr: ${ex.getMessage}")
    }

  def apply(semesterId: String): SemesterId = {
    val (yearStr, semesterStr) = semesterId.splitAt(semesterId.length - 1)
    require(yearStr.toIntOption.isDefined, s"$yearStr should be valid year")
    val semester = parseSemester(semesterStr)
    SemesterId(Year.of(yearStr.toInt), semester)
  }
}
