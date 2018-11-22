package csw.auth.token

import csw.auth.Keycloak
import csw.auth.token.claims.{Access, Audience, Authorization, Permission}
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
    resource_access: Option[Map[String, Access]] = None,
    authorization: Option[Authorization] = None
) {
  def hasPermission(scope: String, resource: String): Boolean =
    this.authorization match {
      case Some(Authorization(Some(perms))) ⇒ perms.exists(p ⇒ p.rsname == resource && p.scopes.exists(_.contains(scope)))
      case _                                ⇒ false
    }

  def hasResourceRole(role: String): Boolean = {
    val clientName: String = Keycloak.deployment.getResourceName
    val maybeRoles = for {
      resourceAccesses ← this.resource_access
      resourceAccess   ← resourceAccesses.get(clientName)
      roles            ← resourceAccess.roles
    } yield roles

    maybeRoles.getOrElse(Set.empty).contains(role)
  }

  def hasRealmRole(role: String): Boolean =
    this.realm_access
      .flatMap(_.roles)
      .getOrElse(Set.empty)
      .contains(role)
}

object AccessToken {
  implicit val accessTokenFormat: OFormat[AccessToken] = Json.format[AccessToken]
}
