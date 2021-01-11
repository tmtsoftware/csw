package csw.params.core.models

import java.util.Optional

/**
 * Represents a unique observation id
 *
 * @param programId represents program Id
 * @param observationNumber Unique observation number in pattern O followed by 3 digit number
 */
case class ObsId(programId: ProgramId, observationNumber: String) {

  /**
   * Returns the ObsId in form of Option
   *
   * @return a defined Option with obsId
   */
  def asOption: Option[ObsId] = Some(new ObsId(programId, observationNumber))

  /**
   * Returns the ObsId in form of Optional
   *
   * @return a defined Optional with obsId
   */
  def asOptional: Optional[ObsId] = Optional.of(new ObsId(programId, observationNumber))

  override def toString: String = s"$programId-$observationNumber"
}

object ObsId {
  private val SEPARATOR = '-'
  def apply(obsId: String): ObsId = {
    require(
      obsId.count(_ == SEPARATOR) == 2,
      s"ObsId must form with semsterId, programNumer, observationNumber separated with \'$SEPARATOR\' ex: 2020A-P001-O123"
    )
    val Array(semesterId, programNumber, observationNumber) = obsId.split(s"\\$SEPARATOR", 3)
    val (fixedPart, number)                                 = observationNumber.splitAt(1)
    require(fixedPart == "O", "Observation Number should start with letter 'O'")
    require(
      number.toIntOption.isDefined && number.length == 3,
      "Observation Number should be valid three digit integer prefixed with letter 'O' ex: O123, O001 etc"
    )
    ObsId(ProgramId(SemesterId(semesterId), programNumber), observationNumber)
  }
}
