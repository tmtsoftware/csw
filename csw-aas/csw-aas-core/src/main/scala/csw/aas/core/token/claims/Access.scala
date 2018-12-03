package csw.aas.core.token.claims

import play.api.libs.json.{Json, OFormat}

private[aas] case class Access(roles: Set[String] = Set.empty)

object Access {

  val empty: Access = Access()

  implicit val accessFormat: OFormat[Access] = Json.using[Json.WithDefaultValues].format
}
