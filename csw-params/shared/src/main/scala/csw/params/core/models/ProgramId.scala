package csw.params.core.models

/**
 * Represents a unique program id
 *
 * @param semesterId semesterId for Program
 * @param programNumber programNumber number in pattern P followed by 3 digit number
 */
case class ProgramId(semesterId: SemesterId, programNumber: Int) {
  require(programNumber >= 1 && programNumber <= 999, "Program Number should be integer in the range of 1 to 999")
  override def toString: String = s"$semesterId-${programNumber.formatted("%03d")}"
}

object ProgramId {
  private val SEPARATOR = "-"
  def apply(programId: String): ProgramId = {
    require(
      programId.contains(SEPARATOR),
      s"ProgramId must form with semsterId, programNumer separated with \'$SEPARATOR\' ex: 2020A-001"
    )
    val Array(semesterId, programNumber) = programId.split(s"\\$SEPARATOR", 2)
    require(programNumber.toIntOption.isDefined, "Program Number should be valid integer ex: 123, 001 etc")
    ProgramId(SemesterId(semesterId), programNumber.toInt)
  }
}
