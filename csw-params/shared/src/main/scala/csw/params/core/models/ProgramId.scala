package csw.params.core.models

/**
 * Represents a unique program id
 *
 * @param semesterId semesterId for Program
 * @param programNumber programNumber number in pattern P followed by 3 digit number
 */
case class ProgramId(semesterId: SemesterId, programNumber: String) {
  val (fixedPart, number) = programNumber.splitAt(1)
  require(fixedPart == "P", "Program Number should start with letter 'P'")
  require(
    number.toIntOption.isDefined && number.length == 3,
    "Program Number should be valid three digit integer prefixed with letter 'P' ex: P123, P001 etc"
  )

  override def toString: String = s"$semesterId-$programNumber"
}

object ProgramId {
  private val SEPARATOR = "-"
  def apply(programId: String): ProgramId = {
    require(
      programId.contains(SEPARATOR),
      s"ProgramId must form with semsterId, programNumer separated with \'$SEPARATOR\' ex: 2020A-P001"
    )
    val Array(semesterId, programNumber) = programId.split(s"\\$SEPARATOR", 2)
    ProgramId(SemesterId(semesterId), programNumber)
  }
}
