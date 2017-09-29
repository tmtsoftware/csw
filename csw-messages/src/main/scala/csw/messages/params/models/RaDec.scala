package csw.messages.params.models

import spray.json.RootJsonFormat

case class RaDec(ra: Double, dec: Double)

case object RaDec {
  import spray.json.DefaultJsonProtocol._

  implicit val raDecFormat: RootJsonFormat[RaDec] = jsonFormat2(RaDec.apply)
}
