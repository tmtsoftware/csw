package csw.params.core.models

import java.util.Optional

/**
 * Represents a unique observation id
 *
 * @param programId represents program Id
 * @param observationNumber Unique observation number in pattern O followed by 3 digit number
 */
case class ObsId(programId: ProgramId, observationNumber: Int) {

  require(observationNumber >= 1 && observationNumber <= 999, "Program Number should be integer in the range of 1 to 999")

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

  override def toString: String = s"$programId-${observationNumber.formatted("%03d")}"
}

object ObsId {
  private val SEPARATOR = '-'
  def apply(obsId: String): ObsId = {
    require(
      obsId.count(_ == SEPARATOR) == 2,
      s"ObsId must form with semsterId, programNumer, observationNumber separated with \'$SEPARATOR\' ex: 2020A-001-123"
    )
    val Array(semesterId, programNumber, observationNumber) = obsId.split(s"\\$SEPARATOR", 3)
    require(
      observationNumber.toIntOption.isDefined,
      "Observation Number should be valid integer prefixed ex: 123, 001 etc"
    )
    ObsId(ProgramId(s"$semesterId$SEPARATOR$programNumber"), observationNumber.toInt)
  }
}
