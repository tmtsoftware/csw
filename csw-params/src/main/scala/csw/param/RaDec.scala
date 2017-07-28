package csw.param

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class RaDec(ra: Double, dec: Double)

case object RaDec extends DefaultJsonProtocol {
  implicit val raDecFormat: RootJsonFormat[RaDec] = jsonFormat2(RaDec.apply)
}
