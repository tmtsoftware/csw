package csw.params.core.models

import csw.params.core.models.ExposureId.SEPARATOR
import csw.prefix.models.Subsystem

case class ExposureId(obsId: ObsId, subsystem: Subsystem, DET: String, tYPLevel: TYPLevel, exposureNumber: ExposureNumber) {

  override def toString: String = s"$obsId$SEPARATOR$subsystem$SEPARATOR$DET$SEPARATOR$tYPLevel$SEPARATOR$exposureNumber"
}

object ExposureId {
  val SEPARATOR               = '-'
  val regexForSeparatingObsId = "[0-9]*([AB])-[0-9]*-[0-9]*-"
  def apply(exposureId: String): ExposureId = {
    validateExposureId(exposureId)

    val Array(_, secondPart)                            = exposureId.split(regexForSeparatingObsId)
    val firstPart: String                               = exposureId.dropRight(secondPart.length)
    val Array(subsystem, det, tYPLevel, exposureNumber) = secondPart.split("-", 4)
    ExposureId(
      ObsId(firstPart.dropRight(1)),
      Subsystem.withNameInsensitive(subsystem),
      det,
      TYPLevel(tYPLevel),
      ExposureNumber(exposureNumber)
    )
  }

  private def validateExposureId(exposureId: String): Unit = {
    require(
      exposureId.matches(s"$regexForSeparatingObsId(.*)"),
      "Invalid exposure Id: ObsId format should be [Year][Semester]-XXX-XXX e.g. 2020A-001-123"
    )

    require(
      exposureId.count(_ == SEPARATOR) >= 6,
      s"Invalid exposure Id: ExposureId should be $SEPARATOR string composing SemesterId-ProgramId-ObservationNumber-Subsystem-DET-TyPLevel-ExposureNumber"
    )
  }
}
