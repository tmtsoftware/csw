package csw.services.logging.commons

import java.time.format.DateTimeFormatter

object Constants {
  val ISOLogFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSSxxxxx")
}
