package csw.auth.core.token.claims

import play.api.libs.json.{Json, OFormat}

private[auth] case class Access(roles: Option[Set[String]])

object Access {
  implicit val accessFormat: OFormat[Access] = Json.format[Access]
}
