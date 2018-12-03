package csw.auth.core.token
import csw.auth.core.Keycloak
import csw.auth.core.commons.AuthLogger
import csw.auth.core.token.claims.{Access, Audience, Authorization}
import play.api.libs.json._

case class AccessToken(
    //standard checks
    sub: Option[String] = None,
    iat: Option[Long] = None,
    exp: Option[Long] = None,
    iss: Option[String] = None,
    aud: Audience = Audience.empty,
    jti: Option[String] = None,
    //additional information
    given_name: Option[String] = None,
    family_name: Option[String] = None,
    name: Option[String] = None,
    preferred_username: Option[String] = None,
    email: Option[String] = None,
    scope: Option[String] = None,
    //auth
    realm_access: Access = Access.empty,
    resource_access: Map[String, Access] = Map.empty,
    authorization: Authorization = Authorization.empty,
    //clientToken
    clientId: Option[String] = None,
    clientAddress: Option[String] = None,
    clientHost: Option[String] = None
) {

  private val logger = AuthLogger.getLogger
  import logger._

  def hasPermission(scope: String, resource: String = "Default Resource"): Boolean = {
    val result = authorization.permissions.exists(p ⇒ p.rsname == resource && p.scopes.contains(scope))
    debug(s"'$userOrClientName' doesn't have permission `$scope` for resource `$resource`")
    result
  }

  def hasResourceRole(role: String): Boolean = {
    val clientName: String = Keycloak.deployment.getResourceName
    val result = this.resource_access.get(clientName) match {
      case Some(access) => access.roles.contains(role)
      case _            => false
    }
    debug(s"'$userOrClientName' doesn't have resource role `$role` for client `$clientName`")
    result
  }

  def hasRealmRole(role: String): Boolean = {
    val result = this.realm_access.roles.contains(role)
    debug(s"'$userOrClientName' doesn't have realm role `$role`")
    result
  }

  private val unknownUser = "Unknown"

  def userOrClientName: String = (preferred_username, clientId) match {
    case (Some(u), _)    ⇒ u
    case (None, Some(c)) ⇒ c
    case _               ⇒ unknownUser
  }
}

object AccessToken {
  implicit val accessTokenFormat: OFormat[AccessToken] = Json.using[Json.WithDefaultValues].format
}
