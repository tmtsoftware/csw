package csw.auth.core.token.claims

import play.api.libs.json.{Json, OFormat}

private[auth] case class Authorization(permissions: Option[Set[Permission]])

private[auth] object Authorization {
  implicit val authorizationFormat: OFormat[Authorization] = Json.format
}
