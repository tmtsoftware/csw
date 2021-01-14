package csw.params.core.models

import java.text.SimpleDateFormat
import java.util.Date

import csw.params.core.models.StandaloneExposureId.SEPARATOR
import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime

case class StandaloneExposureId(
    utcTime: UTCTime,
    subsystem: Subsystem,
    det: String,
    typLevel: TYPLevel,
    exposureNumber: ExposureNumber
) {
  val utcTimeStr: String        = new SimpleDateFormat("YYYYMMDD-HHmmss").format(Date.from(utcTime.value))
  override def toString: String = s"$utcTimeStr$SEPARATOR$subsystem$SEPARATOR$det$SEPARATOR$TYPLevel$SEPARATOR$ExposureNumber"
}

object StandaloneExposureId {
  val SEPARATOR = '-'
  def apply(utcTime: UTCTime, exposureIdRemainingStr: String): StandaloneExposureId = {
    require(
      exposureIdRemainingStr.count(_ == SEPARATOR) >= 3,
      s"Invalid StandaloneExposureId Id: StandaloneExposureId should be $SEPARATOR string composing YYYYMMDD-HHMMSS-Subsystem-DET-TyPLevel-ExposureNumber"
    )
    val Array(subsystem, det, typLevel, exposureNumber) = exposureIdRemainingStr.split("-", 4)
    StandaloneExposureId(
      utcTime,
      Subsystem.withNameInsensitive(subsystem),
      det,
      TYPLevel(typLevel),
      ExposureNumber(exposureNumber)
    )
  }
}
