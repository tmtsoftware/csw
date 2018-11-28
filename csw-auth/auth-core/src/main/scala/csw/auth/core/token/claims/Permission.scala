package csw.auth.core.token.claims

import play.api.libs.json.{Json, OFormat}

private[auth] case class Permission(rsid: String, rsname: String, scopes: Option[Set[String]])

private[auth] object Permission {
  implicit val permissionFormat: OFormat[Permission] = Json.format
}
