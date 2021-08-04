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
  def apply(obsId: String): ObsId =
    obsId.split(Separator.Hyphen) match {
      case Array(semesterId, programNumber, obsNumber) if obsNumber.toIntOption.isDefined =>
        ObsId(ProgramId(Separator.hyphenate(semesterId, programNumber)), obsNumber.toInt)
      case _ =>
        throw new IllegalArgumentException(
          s"An ObsId must consist of a semesterId, programNumber, and observationNumber separated by '${Separator.Hyphen}' ex: 2020A-001-123"
        )
    }
}
