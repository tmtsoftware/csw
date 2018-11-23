package csw.auth.core.token.claims

import play.api.libs.json.{Json, OFormat}

private[auth] case class Access(roles: Set[String] = Set.empty)

object Access {
  implicit val accessFormat: OFormat[Access] = Json.using[Json.WithDefaultValues].format
}
