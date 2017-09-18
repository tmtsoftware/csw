package csw.messages.params.models

import com.trueaccord.scalapb.TypeMapper
import csw_params.radec.PbRaDec
import spray.json.RootJsonFormat

case class RaDec(ra: Double, dec: Double)

case object RaDec {
  import spray.json.DefaultJsonProtocol._

  implicit val raDecFormat: RootJsonFormat[RaDec] = jsonFormat2(RaDec.apply)

  implicit val typeMapper: TypeMapper[PbRaDec, RaDec] =
    TypeMapper[PbRaDec, RaDec](x ⇒ RaDec(x.ra, x.dec))(x ⇒ PbRaDec().withRa(x.ra).withDec(x.dec))
}
