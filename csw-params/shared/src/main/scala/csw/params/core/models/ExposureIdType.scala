package csw.params.core.models

import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime

import java.time.ZoneId

/**
 * ExposureId is an identifier in ESW/DMS for a single exposure.
 * The ExposureId follows the structure: 2020A-001-123-WFOS-IMG1-SCI0-0001 with
 * an included ObsId or when no ObsId is present, in the standalone
 * format: 20200706-190204-WFOS-IMG1-SCI0-0001 with a UTC time when the
 * ExposureId is created.
 */
sealed trait ExposureId {

  /**
   * The Observation Id for the exposure.
   * @return an [[ObsId]] as an option
   */
  def obsId: Option[ObsId]

  /**
   * The Subsystem that produced the exposure.
   *  @return a valid [[Subsystem]]
   */
  def subsystem: Subsystem

  /**
   * The detector name associated with the exposure.
   * @return detector description as a [[String]]
   */
  def det: String

  /**
   * The exposure type and calibration level
   * @return a [[TYPLevel]]
   */
  def typLevel: TYPLevel

  /**
   * The number of the exposure in a series.
   * @return the number as an [[ExposureNumber]]
   */
  def exposureNumber: ExposureNumber
}

/**
 * A standalone ExposureId is an exposureId without an ObsId.
 * Instances are created using the ExposureId object.
 */
case class StandaloneExposureId private[csw] (
    utcTime: UTCTime,
    subsystem: Subsystem,
    det: String,
    typLevel: TYPLevel,
    exposureNumber: ExposureNumber
) extends ExposureId {

  val obsId: Option[ObsId] = None

  override def toString: String =
    Separator.hyphenate(s"${ExposureId.utcAsStandaloneString(utcTime)}", s"$subsystem", s"$det", s"$typLevel", s"$exposureNumber")
}

/**
 * An ExposureIdWithObsId is an ExposureId with an included ObsId.
 * Instances are created through the ExposureId object.
 */
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

/** Factory for ExposureId instances and helper functions. */
object ExposureId {
  import java.time.format.DateTimeFormatter

  // Used to format standalone ExposureId
  private val exposureIdPattern = "uMMdd-HHmmss"
  private val dateTimeFormatter = DateTimeFormatter.ofPattern(exposureIdPattern).withZone(ZoneId.of("UTC"))

  /**
   * A convenience function to create a new ExposureId with the next higher exposure number.
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0001 => 2020A-001-123-WFOS-IMG1-SCI0-0002
   * @param exposureId current ExposureId
   * @return ExposureId with next higher ExposureNumber
   */
  def nextExposureNumber(exposureId: ExposureId): ExposureId =
    updateExposureNumber(exposureId, exposureId.exposureNumber.next())

  /**
   * A convenience function to create a new ExposureId with the next higher exposure number.
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0001 => 2020A-001-123-WFOS-IMG1-SCI0-0002-00.
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0002-00 => 2020A-001-123-WFOS-IMG1-SCI0-0002-01.
   * @param exposureId current ExposureId
   * @return ExposureId with next higher ExposureNumber
   */
  def nextSubArrayNumber(exposureId: ExposureId): ExposureId =
    updateExposureNumber(exposureId, exposureId.exposureNumber.nextSubArray())

  /** Updates ExposureId with new ExposureNumber */
  private def updateExposureNumber(exposureId: ExposureId, update: ExposureNumber): ExposureId =
    exposureId match {
      case expId: ExposureIdWithObsId =>
        expId.copy(exposureNumber = update)
      case expId: StandaloneExposureId =>
        expId.copy(exposureNumber = update)
    }

  /**
   * A convenience function to create a new ExposureId with a new ObsId object.
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0001 => 2020A-001-228-WFOS-IMG1-SCI0-0001.
   * Note that a standalone ExposureId will be changed to an ExposureId with an ObsId
   * @param exposureId current ExposureId
   * @param obsId new ObsId as an [[ObsId]]
   * @return a new ExposureId with given new ObsId
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
   * A convenience function to create a new ExposureId with a new ObsId as a String.
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0001 => 2020A-001-228-WFOS-IMG1-SCI0-0001.
   * Note that a standalone ExposureId will be changed to an ExposureId with an ObsId.
   * @param exposureId current ExposureId
   * @param obsIdString new ObsId as a String
   * @return ExposureId with given new [[ObsId]]
   */
  def withObsId(exposureId: ExposureId, obsIdString: String): ExposureId =
    withObsId(exposureId, ObsId(obsIdString))

  /**
   * A convenience function that allows creating a standalone ExposureId at a specific UTC date and time.
   * Note than an ExposureId with an ObsId can be changed to a standalone ExposureId.
   * @param exposureId current ExposureId
   * @param utc a [[UTCTime]] for the ExposureId
   * @return a standalone ExposureId at the provided UTC
   */
  def withUTC(exposureId: ExposureId, utc: UTCTime): ExposureId =
    StandaloneExposureId(utc, exposureId.subsystem, exposureId.det, exposureId.typLevel, exposureId.exposureNumber)

  /**
   * The UTC time formatted as needed for a standalone ExposureId: YYYYMMDD-HHMMSS.
   * @return  the UTCTime formatted String
   */
  def utcAsStandaloneString(utcTime: UTCTime): String = dateTimeFormatter.format(utcTime.value)

  /**
   * Create an ExposureId from a String of the 4 forms with and without an ObsId and with and without a subarray:
   * IRIS-IMG-SCI0-0001,IRIS-IMG-SCI0-0001-02 when no ObsId is present. Or
   * 2020A-001-123-IRIS-IMG-SCI0-0001 or 2020A-001-123-IRIS-IMG-SCI0-0001-02 when an ObsId is present.
   * @param exposureId proper ExposureId as a String
   * @return instance of ExposureId
   * @throws IllegalArgumentException if the String does not follow the correct structure
   */
  def apply(exposureId: String): ExposureId = {
    val maxArgs: Int = 8
    exposureId.split(Separator.Hyphen, maxArgs) match {
      case Array(obs1, obs2, obs3, subsystemStr, detStr, typStr, expNumStr, subArrayStr) =>
        // This is the case with an ObsId and a sub array
        ExposureIdWithObsId(
          Some(ObsId(Separator.hyphenate(obs1, obs2, obs3))),
          Subsystem.withNameInsensitive(subsystemStr),
          detStr,
          TYPLevel(typStr),
          ExposureNumber(expNumStr + Separator.Hyphen + subArrayStr)
        )
      case Array(obs1, obs2, obs3, subsystemStr, detStr, typStr, expNumStr) =>
        // This is the case with an ObsId and no subarray
        ExposureIdWithObsId(
          Some(ObsId(Separator.hyphenate(obs1, obs2, obs3))),
          Subsystem.withNameInsensitive(subsystemStr),
          detStr,
          TYPLevel(typStr),
          ExposureNumber(expNumStr)
        )
      case Array(subsystemStr, detStr, typStr, expNumStr, subArrayStr) =>
        // This is standalone with subarray
        StandaloneExposureId(
          UTCTime.now(),
          Subsystem.withNameInsensitive(subsystemStr),
          detStr,
          TYPLevel(typStr),
          ExposureNumber(expNumStr + Separator.Hyphen + subArrayStr)
        )
      case Array(subsystemStr, detStr, typStr, expNumStr) =>
        // This is standalone with no subarray
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

  /**
   * This creates a stand-alone ExposureId for the case when there is no [[ObsId]] available.
   * @param subsystem [[Subsystem]] associated with exposure
   * @param det a valid detector String
   * @param typLevel the exposure's [[TYPLevel]]
   * @param exposureNumber the exposure's Exposure Number [[ExposureNumber]]
   * @return A stand-alone ExposureId
   */
  def apply(subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber): ExposureId =
    StandaloneExposureId(UTCTime.now(), subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber)

  /**
   * This creates an ExposureId with an ObsId.
   * @param obsId a valid [[ObsId]]
   * @param subsystem [[Subsystem]] associated with exposure
   * @param det a valid detector String
   * @param typLevel the exposure's [[TYPLevel]]
   * @param exposureNumber the exposure's Exposure Number [[ExposureNumber]]
   * @return A standalone ExposureId
   */
  def apply(obsId: ObsId, subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber): ExposureId =
    ExposureIdWithObsId(Some(obsId), subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber)
}
