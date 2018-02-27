package csw.services.logging.commons

import java.time._
import java.time.format.DateTimeFormatter

//TODO: explain better significance
object TMTDateTimeFormatter {
  val ISOLogFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)

  def format(time: Long): String = {
    ISOLogFormatter.format(Instant.ofEpochMilli(time))
  }

  def parse(dateStr: String): ZonedDateTime = {
    ZonedDateTime.parse(dateStr, ISOLogFormatter)
  }

}
