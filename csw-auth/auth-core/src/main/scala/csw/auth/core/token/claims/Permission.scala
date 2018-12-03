package csw.auth.core.token.claims

import play.api.libs.json.{Json, OFormat}

private[auth] case class Permission(rsid: String, rsname: String, scopes: Set[String] = Set.empty)

private[auth] object Permission {
  implicit val permissionFormat: OFormat[Permission] = Json.using[Json.WithDefaultValues].format
}
