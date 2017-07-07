package csw.services.logging.commons

import java.time._
import java.time.format.DateTimeFormatter

object TMTDateTimeFormatter {
  val ISOLogFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:n").withZone(ZoneOffset.UTC)

  def format(time: Long): String = {
    ISOLogFormatter.format(Instant.ofEpochMilli(time))
  }

  def parse(dateStr: String): ZonedDateTime = {
    ZonedDateTime.parse(dateStr, ISOLogFormatter)
  }

}
