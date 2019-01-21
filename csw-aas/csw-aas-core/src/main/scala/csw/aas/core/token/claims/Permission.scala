package csw.aas.core.token.claims

import play.api.libs.json.{Json, OFormat}

case class Permission(rsid: String, rsname: String, scopes: Set[String] = Set.empty)

object Permission {
  implicit val permissionFormat: OFormat[Permission] = Json.using[Json.WithDefaultValues].format
}
