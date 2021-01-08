package csw.params.core.models

case class ProgramId(semesterId: SemesterId, programNumber: String) {
  val (fixedPart, number) = programNumber.splitAt(1)
  require(fixedPart == "P", "Program Number should start with letter 'P'")
  require(
    number.toIntOption.isDefined && number.length == 3,
    "Program Number should be valid three digit integer prefixed with letter 'P' ex: P123, P001 etc"
  )

  override def toString: String = s"$semesterId-$programNumber"
}
