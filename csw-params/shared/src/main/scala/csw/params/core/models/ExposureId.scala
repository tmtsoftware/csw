package csw.params.core.models

import csw.prefix.models.Subsystem

case class ExposureId(obsId: ObsId, subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber) {
  override def toString: String =
    Separator.hyphenate(s"$obsId", s"$subsystem", s"$det", s"$typLevel", s"$exposureNumber")
}

object ExposureId {
  def apply(exposureId: String): ExposureId =
    exposureId.split(Separator.Hyphen, 7) match {
      case Array(obs1, obs2, obs3, subsystemStr, detStr, typStr, expNumStr) =>
        ExposureId(
          ObsId(Separator.hyphenate(obs1, obs2, obs3)),
          Subsystem.withNameInsensitive(subsystemStr),
          detStr,
          TYPLevel(typStr),
          ExposureNumber(expNumStr)
        )
      case _ =>
        throw new IllegalArgumentException(
          s"requirement failed: Invalid exposure Id: ExposureId should be ${Separator.Hyphen} string " +
            s"composing SemesterId-ProgramId-ObservationNumber-Subsystem-DET-TYPLevel-ExposureNumber"
        )
    }
}
