package csw.auth
import play.api.libs.json.{Json, OFormat}

private[auth] case class Permission(rsid: String, rsname: String, scopes: Option[Set[String]])

object Permission {
  implicit val permissionFormat: OFormat[Permission] = Json.format
}
