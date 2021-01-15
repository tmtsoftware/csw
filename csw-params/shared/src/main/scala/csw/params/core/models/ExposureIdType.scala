package csw.params.core.models

import csw.prefix.models.Subsystem

sealed trait ExposureIdType

case class ExposureId(obsId: ObsId, subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber) {
  override def toString: String =
    Separator.hyphenate(s"$obsId", s"$subsystem", s"$det", s"$typLevel", s"$exposureNumber")
}

case class StandaloneExposureId(
    utcTime: String,
    subsystem: Subsystem,
    det: String,
    typLevel: TYPLevel,
    exposureNumber: ExposureNumber
) {
  override def toString: String = Separator.hyphenate(utcTime, s"$subsystem", det, s"$typLevel", s"$exposureNumber")
}
