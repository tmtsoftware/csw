package csw.auth
import play.api.libs.json.{Json, OFormat}

private[auth] case class Authorization(permissions: Option[Set[Permission]])

object Authorization {
  implicit val authorizationFormat: OFormat[Authorization] = Json.format
}
