package csw.messages.params.models

import com.trueaccord.scalapb.TypeMapper
import csw_messages_params.radec.PbRaDec
import play.api.libs.json.{Json, OFormat}

case class RaDec(ra: Double, dec: Double)

case object RaDec {

  implicit val raDecFormat: OFormat[RaDec] = Json.format[RaDec]

  implicit val typeMapper: TypeMapper[PbRaDec, RaDec] =
    TypeMapper[PbRaDec, RaDec](x ⇒ RaDec(x.ra, x.dec))(x ⇒ PbRaDec().withRa(x.ra).withDec(x.dec))
}
