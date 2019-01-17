package csw.logging.client.commons

import java.time._
import java.time.format.DateTimeFormatter

/**
 * TMTDateTimeFormatter has a DateTimeFormatter of UTC timezone. It uses this DateTimeFormatter to format the time and parse
 * the string presenting a time.
 */
object TMTDateTimeFormatter {
  private val ISOLogFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

  /**
   * Use `DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)` to format the provided time
   *
   * @param time the time needed to format and write in logs
   * @return a string representation of time provided in long
   */
  def format(time: Long): String = ISOLogFormatter.format(Instant.ofEpochMilli(time))

  /**
   * Use `DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)` to parse the provided time
   *
   * @param dateStr the time in string format that need to parse
   * @return a ZonedDateTime created out of provided time
   */
  def parse(dateStr: String): ZonedDateTime = ZonedDateTime.parse(dateStr, ISOLogFormatter)

}
