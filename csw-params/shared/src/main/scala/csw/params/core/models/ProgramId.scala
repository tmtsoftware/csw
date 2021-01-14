package csw.params.core.models

/**
 * Represents a unique program id
 *
 * @param semesterId semesterId for Program
 * @param programNumber programNumber number in pattern P followed by 3 digit number
 */
case class ProgramId(semesterId: SemesterId, programNumber: Int) {
  require(programNumber >= 1 && programNumber <= 999, "Program Number should be integer in the range of 1 to 999")
  override def toString: String = Separator.hyphenate(s"$semesterId", programNumber.formatted("%03d"))
}

object ProgramId {
  def apply(programId: String): ProgramId =
    programId.split(Separator.Hyphen) match {
      // require(programNumber.toIntOption.isDefined, "Program Number should be valid integer ex: 123, 001 etc")
      case Array(semesterId, programNumber) if programNumber.toIntOption.isDefined =>
        ProgramId(SemesterId(semesterId), programNumber.toInt)
      case _ =>
        throw new IllegalArgumentException(
          s"requirement failed: ProgramId must form with semesterId, programNumber separated with '${Separator.Hyphen}' ex: 2020A-001"
        )
    }
}
