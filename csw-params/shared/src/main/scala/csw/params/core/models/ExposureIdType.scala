package csw.params.core.models

import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime
import java.time.{Instant, ZoneId}
import scala.util.{Failure, Success, Try}

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
   * @return an [[csw.params.core.models.ObsId]] as an option
   */
  def obsId: Option[ObsId]

  /**
   * The Subsystem that produced the exposure.
   *  @return a valid [[csw.prefix.models.Subsystem]]
   */
  def subsystem: Subsystem

  /**
   * The detector name associated with the exposure.
   * @return detector description as a [[java.lang.String]]
   */
  def det: String

  /**
   * The exposure type and calibration level
   * @return a [[csw.params.core.models.TYPLevel]]
   */
  def typLevel: TYPLevel

  /**
   * The number of the exposure in a series.
   * @return the number as an [[csw.params.core.models.ExposureNumber]]
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
   * A convenience function to create a new ExposureId with a specific exposure number.
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0001 with 3 => 2020A-001-123-WFOS-IMG1-SCI0-0003
   * @param exposureId current ExposureId
   * @param exposureNumber desired exposure number
   * @return ExposureId with specified exposure number
   */
  def withExposureNumber(exposureId: ExposureId, exposureNumber: Int): ExposureId =
    updateExposureNumber(exposureId, ExposureNumber(exposureNumber))

  /**
   * A convenience function to create a new ExposureId with the next higher exposure number.
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0001 => 2020A-001-123-WFOS-IMG1-SCI0-0002
   * @param exposureId current ExposureId
   * @return ExposureId with next higher exposure number
   */
  def nextExposureNumber(exposureId: ExposureId): ExposureId =
    updateExposureNumber(exposureId, exposureId.exposureNumber.next())

  /**
   * A convenience function to create a new ExposureId with the same exposure number and
   * specified sub array number
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0001, 3 => 2020A-001-123-WFOS-IMG1-SCI0-0002-03.
   * Example: 2020A-001-123-WFOS-IMG1-SCI0-0002-00, 4 => 2020A-001-123-WFOS-IMG1-SCI0-0002-04.
   * @param exposureId current ExposureId
   * @param subArrayNumber specified subArray number
   * @return ExposureId with next higher ExposureNumber
   */
  def withSubArrayNumber(exposureId: ExposureId, subArrayNumber: Int): ExposureId =
    updateExposureNumber(exposureId, ExposureNumber(exposureId.exposureNumber.exposureNumber, Some(subArrayNumber)))

  /**
   * A convenience function to create a new ExposureId with the next higher sub array number.
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
   * @param obsId new ObsId as an [[csw.params.core.models.ObsId]]
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
   * @return ExposureId with given new [[csw.params.core.models.ObsId]]
   */
  def withObsId(exposureId: ExposureId, obsIdString: String): ExposureId =
    withObsId(exposureId, ObsId(obsIdString))

  /**
   * A convenience function that allows creating a standalone ExposureId at a specific UTC date and time.
   * Note than an ExposureId with an ObsId can be changed to a standalone ExposureId.
   * @param exposureId current ExposureId
   * @param utc a [[csw.time.core.models.UTCTime]] for the ExposureId
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
   * A helper function that allows creating exposure id from string in java file.
   * @param exposureId proper ExposureId as a String
   * @return instance of ExposureId
   */
  def fromString(exposureId: String): ExposureId = apply(exposureId)

  /**
   * Create an ExposureId from a String of the 4 forms with and without an ObsId and with and without a subarray:
   * IRIS-IMG-SCI0-0001,IRIS-IMG-SCI0-0001-02 when no ObsId is present. Or
   * 2020A-001-123-IRIS-IMG-SCI0-0001 or 2020A-001-123-IRIS-IMG-SCI0-0001-02 when an ObsId is present.
   * @param exposureId proper ExposureId as a String
   * @return instance of ExposureId
   * @throws java.lang.IllegalArgumentException if the String does not follow the correct structure
   */
  def apply(exposureId: String): ExposureId = {
    val maxArgs: Int = 8
    exposureId.split(Separator.Hyphen, maxArgs) match {
      // 8 Args
      case Array(obs1, obs2, obs3, subsystemStr, detStr, typStr, expNumStr, subArrayStr) =>
        // This is the case with an ObsId and a sub array
        ExposureIdWithObsId(
          Some(ObsId(Separator.hyphenate(obs1, obs2, obs3))),
          Subsystem.withNameInsensitive(subsystemStr),
          detStr,
          TYPLevel(typStr),
          ExposureNumber(expNumStr + Separator.Hyphen + subArrayStr)
        )
      // 7 args
      case Array(p1, p2, p3, p4, p5, p6, p7) =>
        // This is the case with an ObsId and no subarray
        // Or Standalone with subarray
        // If it is with ObsId, the first part with be a semester ID which is always length 5
        if (p1.length == 5) {
          ExposureIdWithObsId(
            Some(ObsId(Separator.hyphenate(p1, p2, p3))),
            Subsystem.withNameInsensitive(p4),
            p5,
            TYPLevel(p6),
            ExposureNumber(p7)
          )
        }
        else {
          // It is a standalone with a subarray
          toTimeDateAtUTC(p1, p2) match {
            case Success(utcTime) =>
              StandaloneExposureId(
                utcTime,
                Subsystem.withNameInsensitive(p3),
                p4,
                TYPLevel(p5),
                ExposureNumber(p6 + Separator.Hyphen + p7)
              )
            case Failure(ex) =>
              throw ex
          }
        }
      case Array(date, time, subsystemStr, detStr, typStr, expNumStr) if (date.length != 5) =>
        // 6 args - first two should be UTC time
        toTimeDateAtUTC(date, time) match {
          case Success(utcTime) =>
            StandaloneExposureId(
              utcTime,
              Subsystem.withNameInsensitive(subsystemStr),
              detStr,
              TYPLevel(typStr),
              ExposureNumber(expNumStr)
            )
          case Failure(ex) =>
            throw ex
        }
      case Array(subsystemStr, detStr, typStr, expNumStr, subArrayStr) =>
        // 5 args = this is standalone with subarray
        StandaloneExposureId(
          UTCTime.now(),
          Subsystem.withNameInsensitive(subsystemStr),
          detStr,
          TYPLevel(typStr),
          ExposureNumber(expNumStr + Separator.Hyphen + subArrayStr)
        )
      // 4 args
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

  /** Convert an input date and time string to an Instant.  Throws parse exception on failure */
  private def toTimeDateAtUTC(dateStr: String, timeStr: String): Try[UTCTime] = Try {
    UTCTime(Instant.from(dateTimeFormatter.parse(s"$dateStr-$timeStr")))
  }

  /**
   * This creates a stand-alone ExposureId for the case when there is no [[csw.params.core.models.ObsId]] available.
   * @param subsystem [[csw.prefix.models.Subsystem]] associated with exposure
   * @param det a valid detector String
   * @param typLevel the exposure's [[csw.params.core.models.TYPLevel]]
   * @param exposureNumber the exposure's Exposure Number [[csw.params.core.models.ExposureNumber]]
   * @return A stand-alone ExposureId
   */
  def apply(subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber): ExposureId =
    StandaloneExposureId(UTCTime.now(), subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber)

  /**
   * This creates an ExposureId with an ObsId.
   * @param obsId a valid [[csw.params.core.models.ObsId]]
   * @param subsystem [[csw.prefix.models.Subsystem]] associated with exposure
   * @param det a valid detector String
   * @param typLevel the exposure's [[csw.params.core.models.TYPLevel]]
   * @param exposureNumber the exposure's Exposure Number [[csw.params.core.models.ExposureNumber]]
   * @return A standalone ExposureId
   */
  def apply(obsId: ObsId, subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber): ExposureId =
    ExposureIdWithObsId(Some(obsId), subsystem: Subsystem, det: String, typLevel: TYPLevel, exposureNumber: ExposureNumber)
}
