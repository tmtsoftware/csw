package csw.aas.core.token.claims

import play.api.libs.json.{Json, OFormat}

case class Authorization(permissions: Set[Permission] = Set.empty)

object Authorization {
  val empty: Authorization = Authorization()

  implicit val authorizationFormat: OFormat[Authorization] = Json.using[Json.WithDefaultValues].format
}
