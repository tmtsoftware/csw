package csw.params.core.models

import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime

import java.text.SimpleDateFormat
import java.util.Date

case class StandaloneExposureId(
    utcTime: UTCTime,
    subsystem: Subsystem,
    det: String,
    typLevel: TYPLevel,
    exposureNumber: ExposureNumber
) {
  val utcTimeStr: String        = new SimpleDateFormat("YYYYMMDD-HHmmss").format(Date.from(utcTime.value))
  override def toString: String = Separator.hyphenate(s"$utcTimeStr", s"$subsystem", s"$det", s"$typLevel", s"$exposureNumber")
}

object StandaloneExposureId {
  def apply(utcTime: UTCTime, exposureIdRemainingStr: String): StandaloneExposureId =
    exposureIdRemainingStr.split(Separator.Hyphen, 4) match {
      case Array(subsystem, det, typLevel, exposureNumber) =>
        StandaloneExposureId(
          utcTime,
          Subsystem.withNameInsensitive(subsystem),
          det,
          TYPLevel(typLevel),
          ExposureNumber(exposureNumber)
        )
      case _ =>
        throw new IllegalArgumentException(
          s"Invalid StandaloneExposureId Id: StandaloneExposureId should be ${Separator.Hyphen} string " +
            s"composing YYYYMMDD-HHMMSS-Subsystem-DET-TYPLevel-ExposureNumber"
        )
    }

}
