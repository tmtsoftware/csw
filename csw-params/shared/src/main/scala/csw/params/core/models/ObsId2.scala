package csw.params.core.models

import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.util.matching.Regex.Match

/**
 * Supports ObsID of the form 2022A|B-Q|P|E|C-ProgNum-ObsNum-XFileNum
 *                            YYYY(A|B)-(Q|C)-PXXX-OXXX-DXXXX
 *                            0123 4   5 6   7890123456789
 *                                              1111111111
 */
sealed trait ProgramKind
case object Classical            extends ProgramKind
case object PreProgrammedQueue   extends ProgramKind
case object ConditionsBasedQueue extends ProgramKind

sealed trait Semester
case object SemesterA extends Semester
case object SemesterB extends Semester

case class ObsID2(year: String, sem: String, kind: String, prog: String, obs: String, detPlusfile: Option[String]) {
  // private final val PROG_KIND_INDEX = 6

  private val ClassicalKindLetter          = 'C'
  private val PreProgrammedQueueKindLetter = 'P'
  private val conditionsBasedQueueLetter   = 'Q'

  //def isQueue = sem.charAt(PROG_KIND_INDEX) == PreProgrammedQueueKindLetter

  //def isClassical = obsId.charAt(PROG_KIND_INDEX) == ClassicalKindLetter
  /*
  def progType: ProgramKind = obsId.charAt(PROG_KIND_INDEX) match {
    case ClassicalKindLetter => Classical
    case PreProgrammedQueueKindLetter => PreProgrammedQueue
  }
 */
}

case class ObsId3(obsId: String) {
  //YYYY(A|B)-(Q|C|E)-PXXX-OXXX-XXXX
  val s: Array[String] = obsId.split("-")
  val isValid          = s.length >= 4
  val hasFile          = s.length == 5


  def semester: String    = if (isValid) s(0) else ""
  def year: String        = if (isValid) semester.substring(0, 4) else ""
  def whichSemester: Char = if (isValid) semester.last else ' '
  def programType: Char   = if (isValid) s(1).charAt(0) else ' '
  def program: String     = if (isValid) s(2) else ""
  def observation: String = if (isValid) s(3) else ""
  def file: String        = if (hasFile) s(4) else ""
  def detector: Char      = if (hasFile) file.head else ' '

  private def validPrint():String = {
    if (isValid) {
      s"$year$whichSemester-$programType-$program-$observation${if(hasFile) s"-$file"}"
    } else obsId
  }

  override def toString = if(!isValid) obsId else validPrint
}

object ObsID2 {
  val BAD_OBSID = ObsID2("bad", "obs", "id", "bad", "obs", Some("id"))

  //YYYY(A|B|E)-(Q|C)-PXXX-OXXX-XXXX
  val reg = raw"(\d{4})(A|B|E)-(C|Q)-P(\d{1,3})-O(\d{1,3})(?:-)?(\w?\d+)?".r

  implicit def create(obsId: String): ObsID2 = obsId match {
    case reg(year, sem, kind, prog, obs, null) =>
      println(s"Its: $year $sem with $kind and prog $prog, obs $obs no file")
      ObsID2(year, sem, kind, prog, obs, None)
    case reg(year, sem, kind, prog, obs, file) =>
      println(s"Its: $year $sem with $kind and prog $prog, obs $obs, file $file")
      ObsID2(year, sem, kind, prog, obs, Some(file))
    case _ =>
      println("Fail")
      BAD_OBSID
  }
}
