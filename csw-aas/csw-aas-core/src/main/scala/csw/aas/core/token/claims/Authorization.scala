package csw.aas.core.token.claims

import play.api.libs.json.{Json, OFormat}

private[aas] case class Authorization(permissions: Set[Permission] = Set.empty)

private[aas] object Authorization {
  val empty: Authorization = Authorization()

  implicit val authorizationFormat: OFormat[Authorization] = Json.using[Json.WithDefaultValues].format
}
