package csw.messages.params.models

import com.trueaccord.scalapb.TypeMapper
import csw_protobuf.radec.PbRaDec
import play.api.libs.json.{Json, OFormat}

/**
 * Holds Ra(Right Ascension) and Dec(Declination) values
 */
case class RaDec(ra: Double, dec: Double)

case object RaDec {

  //used by play-json
  implicit val raDecFormat: OFormat[RaDec] = Json.format[RaDec]

  //used by Protobuf for conversion between RaDec <=> PbRaDec
  implicit val typeMapper: TypeMapper[PbRaDec, RaDec] =
    TypeMapper[PbRaDec, RaDec](x ⇒ RaDec(x.ra, x.dec))(x ⇒ PbRaDec().withRa(x.ra).withDec(x.dec))
}
