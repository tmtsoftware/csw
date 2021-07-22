package csw.params.core.models

import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime

import java.time.ZoneId
import java.util.Optional

/**
 * ExposureId is the identifier in ESW/DMS for a single exposure
 * Every ExposureId impl must provide this interface.
 */
sealed trait ExposureId {
  def obsId: Option[ObsId]
  def subsystem: Subsystem
  def det: String
  def typLevel: TYPLevel
  def exposureNumber: ExposureNumber
}

/**
 * A Standalone ExposureId is an exposureId without an ObsId
 * @param utcTime UTC time to associate with Exposure ID
 * @param subsystem [Subsystem] associated with exposure
 * @param det detector string
 * @param typLevel exposure's [TYPLevel]
 * @param exposureNumber exposure's Exposure Number
 */
case class StandaloneExposureId private[csw] (
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

  def atUTC(newUtcTime: UTCTime): StandaloneExposureId =
    copy(utcTime = newUtcTime)

  def utcAsString: String = dateTimeFormatter.format(utcTime.value)

  override def toString: String =
    Separator.hyphenate(s"$utcAsString", s"$subsystem", s"$det", s"$typLevel", s"$exposureNumber")
}

case class ExposureIdWithObsId private[csw] (
    obsId: Option[ObsId],
    subsystem: Subsystem,
    det: String,
    typLevel: TYPLevel,
    exposureNumber: ExposureNumber
) extends ExposureId {

  override def toString: String =
    Separator.hyphenate(s"${obsId.get}", s"$subsystem", s"$det", s"$typLevel", s"$exposureNumber")
}

object ExposureId {

  /**
   * A convenience function to create a new ExposureId with the next higher exposure number
   * 2020A-001-123-WFOS-IMG1-SCI0-0001 => 2020A-001-123-WFOS-IMG1-SCI0-0002
   * @param exposureId current ExposureId
   * @return ExposureId with next higher ExposureNumber
   */
  def nextExposureNumber(exposureId: ExposureId): ExposureId = {
    val next = exposureId.exposureNumber.next()
    exposureId match {
      case expId: ExposureIdWithObsId =>
        expId.copy(exposureNumber = next)
      case expId: StandaloneExposureId =>
        expId.copy(exposureNumber = next)
    }
  }

  /**
   * A convenience function to create a new ExposureId with a new ObsId object
   * 2020A-001-123-WFOS-IMG1-SCI0-0001 => 2020A-001-128-WFOS-IMG1-SCI0-0001
   * Note that a standalone ExposureId will be changed to an ExposureId with an ObsId
   * @param exposureId current ExposureId
   * @param obsId new ObsId as an [[ObsId]]
   * @return ExposureId with given new ObsId
   */
  def withObsId(exposureId: ExposureId, obsId: ObsId): ExposureId = {
    exposureId match {
      case expId: ExposureIdWithObsId =>
        expId.copy(obsId = Some(obsId))
      case _: StandaloneExposureId =>
        ExposureIdWithObsId(Some(obsId), exposureId.subsystem, exposureId.det, exposureId.typLevel, exposureId.exposureNumber)
    }
  }

  /**
   * A convenience function to create a new ExposureId with a new ObsId as a String
   * 2020A-001-123-WFOS-IMG1-SCI0-0001 => 2020A-001-128-WFOS-IMG1-SCI0-0001
   * Note that a standalone ExposureId will be changed to an ExposureId with an ObsId
   * @param exposureId current ExposureId
   * @param obsIdString new ObsId as an [[String]]
   * @return ExposureId with given new ObsId
   */
  def withObsId(exposureId: ExposureId, obsIdString: String): ExposureId =
    withObsId(exposureId, ObsId(obsIdString))

  /**
   * Create an ExposureId from a String of the form CSW-IMG1-SCI0-0001 or
   * 2020A-001-123-CSW-IMG1-SCI0-0001
   * This throws various [[IllegalArgumentException]] if the String does not follow the format
   * @param exposureId proper ExposureId String
   * @return instance of ExposureId
   */
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

  /** This is
   * @param subsystem
   * @param det
   * @param typLevel
   * @param exposureNumber
   * @return
   */
  def apply(subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber): ExposureId =
    StandaloneExposureId(UTCTime.now(), subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber)

  /**
   * @param obsId
   * @param subsystem
   * @param det
   * @param typLevel
   * @param exposureNumber
   * @return
   */
  def apply(obsId: ObsId, subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber): ExposureId =
    ExposureIdWithObsId(Some(obsId), subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber)
}
