package csw.auth
import play.api.libs.json.{Json, OFormat}

private[auth] sealed trait Access {
  val roles: Option[Set[String]]
}

private[auth] final case class RealmAccess(override val roles: Option[Set[String]]) extends Access

private[auth] object RealmAccess {
  implicit val accessFormat: OFormat[RealmAccess] = Json.format[RealmAccess]
}

private[auth] final case class ResourceAccess(override val roles: Option[Set[String]]) extends Access

private[auth] object ResourceAccess {
  implicit val accessFormat: OFormat[ResourceAccess] = Json.format[ResourceAccess]
}
