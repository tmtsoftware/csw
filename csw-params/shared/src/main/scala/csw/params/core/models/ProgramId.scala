package csw.params.core.models

case class ProgramId(semesterId: SemesterId, programNumber: Int) {
  require(programNumber.toString.length == 3, "Program Number should be of three digit")

  override def toString: String = s"$semesterId-$programNumber"
}
