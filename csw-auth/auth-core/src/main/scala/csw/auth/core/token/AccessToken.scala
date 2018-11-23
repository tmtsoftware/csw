package csw.auth.core.token
import csw.auth.core.Keycloak
import csw.auth.core.token.claims.{Access, Audience, Authorization}
import play.api.libs.json._

//todo: integrate csw logging
case class AccessToken(
    //standard checks
    sub: Option[String] = None,
    iat: Option[Long] = None,
    exp: Option[Long] = None,
    iss: Option[String] = None,
    aud: Option[Audience] = None,
    jti: Option[String] = None,
    //additional information
    given_name: Option[String] = None,
    family_name: Option[String] = None,
    name: Option[String] = None,
    preferred_username: Option[String] = None,
    email: Option[String] = None,
    scope: Option[String] = None,
    //auth
    realm_access: Option[Access] = None,
    resource_access: Map[String, Access] = Map.empty,
    authorization: Option[Authorization] = None
) {
  def hasPermission(scope: String, resource: String): Boolean =
    this.authorization match {
      case Some(Authorization(perms)) ⇒ perms.exists(p ⇒ p.rsname == resource && p.scopes.exists(_.contains(scope)))
      case _                          ⇒ false
    }

  def hasResourceRole(role: String): Boolean = {
    val clientName: String = Keycloak.deployment.getResourceName

    this.resource_access.get(clientName) match {
      case Some(access) => access.roles.contains(role)
      case _            => false
    }
  }

  def hasRealmRole(role: String): Boolean =
    this.realm_access.exists(_.roles.contains(role))
}

object AccessToken {
  implicit val accessTokenFormat: OFormat[AccessToken] = Json.using[Json.WithDefaultValues].format
}
