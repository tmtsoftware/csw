package csw.params.core.models

import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime

import java.time.ZoneId

sealed trait ExposureId {
  def obsId: Option[ObsId]
  def subsystem: Subsystem
  def det: String
  def typLevel: TYPLevel
  def exposureNumber: ExposureNumber
  def withObsId(obsId: ObsId): ExposureId
  def withObsId(obsId: String): ExposureId
}

case class StandaloneExposureId(
    utcTime: UTCTime,
    subsystem: Subsystem,
    det: String,
    typLevel: TYPLevel,
    exposureNumber: ExposureNumber
) extends ExposureId {
  import java.time.format.DateTimeFormatter

  private val exposureIdPattern = "uMMdd-HHmmss"
  private val dateTimeFormatter = DateTimeFormatter.ofPattern(exposureIdPattern).withZone(ZoneId.of("UTC"))

  val obsId: Option[ObsId] = None

  def withObsId(obsId: String): ExposureId =
    ExposureIdWithObsId(Some(ObsId.apply(obsId)), subsystem, det, typLevel, exposureNumber)

  def withObsId(obsId: ObsId): ExposureId =
    ExposureIdWithObsId(Some(obsId), subsystem, det, typLevel, exposureNumber)

  def atUTC(newUtcTime: UTCTime): StandaloneExposureId =
    copy(utcTime = newUtcTime)

  def utcAsString: String = dateTimeFormatter.format(utcTime.value)

  override def toString: String =
    Separator.hyphenate(s"$utcAsString", s"$subsystem", s"$det", s"$typLevel", s"$exposureNumber")
}

case class ExposureIdWithObsId(
    obsId: Option[ObsId],
    subsystem: Subsystem,
    det: String,
    typLevel: TYPLevel,
    exposureNumber: ExposureNumber
) extends ExposureId {

  // Allows creating a new ExposureId with a different ObsId
  def withObsId(newObsId: String): ExposureId =
    this.copy(obsId = Some(ObsId.apply(newObsId)))

  def withObsId(newObsId: ObsId): ExposureId =
    copy(obsId = Some(newObsId))

  override def toString: String =
    Separator.hyphenate(s"${obsId.get}", s"$subsystem", s"$det", s"$typLevel", s"$exposureNumber")

}

object ExposureId {
  def apply(exposureId: String): ExposureId = {
    exposureId.split(Separator.Hyphen, 7) match {
      case Array(obs1, obs2, obs3, subsystemStr, detStr, typStr, expNumStr) =>
        ExposureIdWithObsId(
          Some(ObsId(Separator.hyphenate(obs1, obs2, obs3))),
          Subsystem.withNameInsensitive(subsystemStr),
          detStr,
          TYPLevel(typStr),
          ExposureNumber(expNumStr)
        )
      case Array(subsystemStr, detStr, typStr, expNumStr) =>
        StandaloneExposureId(
          UTCTime.now(),
          Subsystem.withNameInsensitive(subsystemStr),
          detStr,
          TYPLevel(typStr),
          ExposureNumber(expNumStr)
        )
      case _ =>
        throw new IllegalArgumentException(
          s"requirement failed: An ExposureId must be a ${Separator.Hyphen} separated string of the form " +
            "SemesterId-ProgramNumber-ObservationNumber-Subsystem-DET-TYPLevel-ExposureNumber"
        )
    }
  }

  def apply(subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber): ExposureId =
    StandaloneExposureId(UTCTime.now(), subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber)

  def apply(obsId: ObsId, subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber): ExposureId =
    ExposureIdWithObsId(Some(obsId), subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber)
}
