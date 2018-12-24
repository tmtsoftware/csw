package csw.aas.core.token

import csw.aas.core.commons.AuthLogger
import csw.aas.core.token.claims.{Access, Audience, Authorization}
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
    val result = this.authorization.permissions.exists(p ⇒ p.rsname == resource && p.scopes.contains(scope))
    if (!result) debug(s"'$userOrClientName' doesn't have permission '$scope' for resource '$resource'")
    else debug(s"authorization granted for user '$userOrClientName' via permission '$scope' and resource '$resource'")
    result
  }

  def hasResourceRole(role: String, resourceName: String): Boolean = {
    val result = this.resource_access.get(resourceName).exists(_.roles.contains(role))
    if (!result) debug(s"'$userOrClientName' doesn't have resource role '$role' for client '$resourceName'")
    else debug(s"authorization granted for user '$userOrClientName' via resource role '$role' and resource '$resourceName'")
    result
  }

  def hasRealmRole(role: String): Boolean = {
    val result = this.realm_access.roles.contains(role)
    if (!result) debug(s"'$userOrClientName' doesn't have realm role '$role'")
    else debug(s"authorization granted for user '$userOrClientName' via realm role '$role'")
    result
  }

  private val UnknownUser = "Unknown"

  def userOrClientName: String = (preferred_username, clientId) match {
    case (Some(u), _)    ⇒ u
    case (None, Some(c)) ⇒ c
    case _               ⇒ UnknownUser
  }
}

object AccessToken {
  implicit val accessTokenFormat: OFormat[AccessToken] = Json.using[Json.WithDefaultValues].format
}
