package csw.params.core.models

import play.api.libs.json.{Json, OFormat}

/**
 * Holds Ra(Right Ascension) and Dec(Declination) values
 */
case class RaDec(ra: Double, dec: Double)

case object RaDec {

  //used by play-json
  implicit val raDecFormat: OFormat[RaDec] = Json.format[RaDec]
}
