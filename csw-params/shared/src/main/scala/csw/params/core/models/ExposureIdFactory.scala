package csw.params.core.models

import csw.prefix.models.Subsystem

object ExposureIdFactory {
  def makeExposureId(exposureId: String): ExposureId =
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
            s"composing SemesterId-ProgramNumber-ObservationNumber-Subsystem-DET-TYPLevel-ExposureNumber"
        )
    }

  def makeStandaloneExposureId(standaloneExposureId: String): StandaloneExposureId =
    standaloneExposureId.split(Separator.Hyphen, 6) match {
      case Array(date, time, subsystem, det, typLevel, exposureNumber) =>
        StandaloneExposureId(
          Separator.hyphenate(date, time),
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
