package csw.params.core.models

import scala.language.implicitConversions

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

case class ObsId2(year: String, sem: String, kind: String, prog: String, obs: String, detPlusfile: Option[String]) {
  // private final val PROG_KIND_INDEX = 6
  /*
  private val ClassicalKindLetter          = 'C'
  private val PreProgrammedQueueKindLetter = 'P'
  private val conditionsBasedQueueLetter   = 'Q'
*/
  //def isQueue = sem.charAt(PROG_KIND_INDEX) == PreProgrammedQueueKindLetter

  //def isClassical = obsId.charAt(PROG_KIND_INDEX) == ClassicalKindLetter
  /*
  def progType: ProgramKind = obsId.charAt(PROG_KIND_INDEX) match {
    case ClassicalKindLetter => Classical
    case PreProgrammedQueueKindLetter => PreProgrammedQueue
  }
 */
}

object ObsId2 {
  val BAD_OBSID = ObsId2("bad", "obs", "id", "bad", "obs", Some("id"))

  //YYYY(A|B|E)-(Q|C)-PXXX-OXXX-XXXX
  private val reg = raw"(\d{4})(A|B|E)-(C|Q)-P(\d{1,3})-O(\d{1,3})(?:-)?(\w?\d+)?".r

  implicit def create(obsId: String): ObsId2 = obsId match {
    case reg(year, sem, kind, prog, obs, null) =>
      ObsId2(year, sem, kind, prog, obs, None)
    case reg(year, sem, kind, prog, obs, file) =>
      ObsId2(year, sem, kind, prog, obs, Some(file))
    case _ =>
      BAD_OBSID
  }
}

/**
  * This ObsId tries to accept whatever is handed in so current uses of ObsId still work.
  * Needs real types.
  *
  * @param obsId String that could be an obsId
  */
case class ObsId3(obsId: String) {
  //YYYY(A|B)-(Q|C|E)-PXXX-OXXX-[D]XXXX
  private val s: Array[String] = obsId.split("-")
  private val VALID_LENGTH = 4 // Must have this many parts to be valid
  private val FILE_INDEX = 4 // If it has a file it is at this index
  private val FILE_WITH_DETECTOR = 5 // Length of last part if has detector
  val isValid: Boolean = s.length >= VALID_LENGTH
  val hasFile: Boolean = s.length == FILE_WITH_DETECTOR && s(FILE_INDEX).length >= VALID_LENGTH
  val hasDetector: Boolean = hasFile && s(FILE_INDEX).length == FILE_WITH_DETECTOR

  def semester: String    = if (isValid) s(0) else ""
  def year: String        = if (isValid) semester.substring(0, 4) else ""

  def whichSemester: String = if (isValid) semester.last.toString else ""

  def programType: String = if (isValid) s(1).head.toString else ""
  def program: String     = if (isValid) s(2) else ""
  def observation: String = if (isValid) s(3) else ""

  def file: String = if (hasFile) if (hasDetector) s(FILE_INDEX).tail else s(FILE_INDEX) else ""

  def detector: String = if (hasDetector) s(FILE_INDEX).head.toString else ""

  private def obsIdPrint(): String = {
    if (isValid) {
      s"$year$whichSemester-$programType-$program-$observation${if (hasFile) s"-$detector$file" else "" }"
    } else obsId
  }

  override def toString: String = obsIdPrint()
}


