package csw.time.api

import java.time.{Instant, ZoneId, ZoneOffset, ZonedDateTime}

import csw.clock.models.TMTClock.clock
import csw.clock.natives.TimeLibrary
import julienrf.json.derived
import play.api.libs.json._

/**
 * Represents an instantaneous point in time. Its a wrapper around [[java.time.Instant]] and provides nanosecond precision.
 * Supports 2 timescales:
 * - [[UTCTime]] for Coordinated Universal Time (UTC) and
 * - [[TAITime]] for International Atomic Time (TAI)
 */
sealed trait TMTTime extends Product with Serializable {
  def value: Instant
}

object TMTTime {
  implicit val format: OFormat[TMTTime] = derived.flat.oformat((__ \ "type").format[String])
}

/**
 * Represents an instantaneous point in time in the UTC scale.
 * Does not contain zone information. To represent this instance in various zones, APIS such as [[at]], [[atLocal]], [[atHawaii]] could be used.
 *
 * @param value the underlying [[java.time.Instant]]
 */
final case class UTCTime(value: Instant) extends TMTTime {

  /**
   * Converts the [[UTCTime]] to [[TAITime]] by adding the UTC-TAI offset.
   * UTC-TAI offset is fetched by doing a native call to [[TimeLibrary.ntp_gettimex()]]. It ensures to get the latest offset as updated by the PTP Grandmaster.
   *
   * @return TAI time at the given UTC time
   */
  def toTAI: TAITime = TAITime(value.plusSeconds(clock.offset))

  /**
   * Combines the [[UTCTime]] with the given timezone to get a [[java.time.ZonedDateTime]]
   *
   * @param zoneId id of the required zone
   * @return time at the given zone
   */
  def at(zoneId: ZoneId): ZonedDateTime = value.atZone(zoneId)

  /**
   * Combines the [[UTCTime]] with the Local timezone to get a [[java.time.ZonedDateTime]]. Local timezone is the system's default timezone.
   *
   * @return time at the Local zone
   */
  def atLocal: ZonedDateTime = at(ZoneId.systemDefault())

  /**
   * Combines the [[UTCTime]] with the Hawaii timezone to get a [[java.time.ZonedDateTime]].
   *
   * @return time at the Hawaii-Aleutian Standard Time (HST) zone
   */
  def atHawaii: ZonedDateTime = at(ZoneId.of("US/Hawaii"))

  /**
   * Converts the [[UTCTime]] instance to [[java.time.ZonedDateTime]] by adding 0 offset of UTC.
   *
   * @return zoned representation of the UTCTime
   */
  def toZonedDateTime: ZonedDateTime = at(ZoneOffset.UTC)
}

object UTCTime {

  /**
   * Obtains the PTP (Precision Time Protocol) synchronized current UTC time.
   * In case of a Linux machine, this will make a native call [[TimeLibrary.clock_gettime()]] inorder to get time from the system clock with nanosecond precision.
   * In case of all the other operating systems, nanosecond precision is not supported, hence no native call is made.
   *
   * @return current time in UTC scale
   */
  def now(): UTCTime = UTCTime(clock.utcInstant)

  implicit val format: OFormat[UTCTime] = TMTTime.format.asInstanceOf[OFormat[UTCTime]]
}

/**
 * Represents an instantaneous point in International Atomic Time (TAI).
 *
 * @param value the underlying [[java.time.Instant]]
 */
final case class TAITime(value: Instant) extends TMTTime {

  /**
   * Converts the [[TAITime]] to [[UTCTime]] by subtracting the UTC-TAI offset.
   * UTC-TAI offset is fetched by doing a native call to [[TimeLibrary.ntp_gettimex()]]. It ensures to get the latest offset as updated by the PTP Grandmaster.
   *
   * @return UTC time at the given TAI time
   */
  def toUTC: UTCTime = UTCTime(value.minusSeconds(clock.offset))
}

object TAITime {

  /**
   * Obtains the PTP (Precision Time Protocol) synchronized current time in TAI timescale.
   * In case of a Linux machine, this will make a native call [[TimeLibrary.clock_gettime()]] inorder to get time from the system clock with nanosecond precision
   * In case of all the other operating systems, nanosecond precision is not supported, hence no native call is made.
   *
   * @return current time in TAI scale
   */
  def now(): TAITime = TAITime(clock.taiInstant)

  /**
   * Fetches UTC to TAI offset by doing a native call to [[TimeLibrary.ntp_gettimex()]] in case of a Linux machine.
   * It ensures to get the latest offset as updated by the PTP Grandmaster.
   *
   * @return offset of UTC to TAI in seconds
   */
  def offset: Int = clock.offset

  // fixme: only for testing and making it private[api] does not work from java test
  def setOffset(offset: Int): Unit = clock.setTaiOffset(offset)
}
