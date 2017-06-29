package csw.services.logging.commons

import java.time._
import java.time.format.DateTimeFormatter

object TMTDateTimeFormatter {
  val ISOLogFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSxxxxx")

  def format(time: Long): String = {
    val offsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault)
    offsetDateTime.format(ISOLogFormatter)
  }

  def parse(dateStr: String): LocalDateTime = {
    LocalDateTime.parse(dateStr, ISOLogFormatter)
  }

}
