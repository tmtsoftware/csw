package csw.aas.core.token

import csw.aas.core.commons.AuthLogger
import csw.aas.core.token.claims.{Access, Audience, Authorization}
import play.api.libs.json._

/**
 * Represents JSON Web Token (JWT) for TMT.
 * JWTs part of OpenID Connect specification
 * @param sub Subject (whom the token refers to)
 * @param iat Issued at (Seconds since UNIX epoch)
 * @param exp Expiration Time (Seconds since UNIX epoch)
 * @param iss Issuer (Identifies principal that issued the JWT)
 * @param aud Audience (Identifies the recipients that the JWT is intended for)
 * @param jti JWT ID (unique identifier of the token even among different issuers)
 * @param given_name Given name of the user
 * @param family_name Family name of the user
 * @param name Full name of the user
 * @param preferred_username username
 * @param email email address of user
 * @param scope scope specify what access privileges are being requested for Access Tokens
 * @param realm_access Realm roles
 * @param resource_access Resource roles
 * @param authorization Permissions
 * @param clientId Id of client.
 *                 This is present only when request is on behalf of a client and not user
 * @param clientAddress Address of client.
 *                      This is present only when request is on behalf of a client and not user
 * @param clientHost Client host.
 *                   This is present only when request is on behalf of a client and not user
 */
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

  /**
   * Checks whether this access token has given permission or not
   * @param scope scope name
   * @param resource resource name
   */
  def hasPermission(scope: String, resource: String = "Default Resource"): Boolean = {
    val result = this.authorization.permissions.exists(p ⇒ p.rsname == resource && p.scopes.contains(scope))
    if (!result) debug(s"'$userOrClientName' doesn't have permission '$scope' for resource '$resource'")
    else debug(s"authorization granted for user '$userOrClientName' via permission '$scope' and resource '$resource'")
    result
  }

  /**
   * Checks whether this access token has resource role (client specific role) or not
   * @param role role name
   * @param resourceName resource name (client name)
   */
  def hasResourceRole(role: String, resourceName: String): Boolean = {
    val result = this.resource_access.get(resourceName).exists(_.roles.contains(role))
    if (!result) debug(s"'$userOrClientName' doesn't have resource role '$role' for client '$resourceName'")
    else debug(s"authorization granted for user '$userOrClientName' via resource role '$role' and resource '$resourceName'")
    result
  }

  /**
   * Checks whether this access token has realm role or not
   * @param role role name
   */
  def hasRealmRole(role: String): Boolean = {
    val result = this.realm_access.roles.contains(role)
    if (!result) debug(s"'$userOrClientName' doesn't have realm role '$role'")
    else debug(s"authorization granted for user '$userOrClientName' via realm role '$role'")
    result
  }

  private val UnknownUser = "Unknown"

  /**
   * Returns username in case of a user token or client id in case of client token
   */
  def userOrClientName: String = (preferred_username, clientId) match {
    case (Some(u), _)    ⇒ u
    case (None, Some(c)) ⇒ c
    case _               ⇒ UnknownUser
  }
}

object AccessToken {
  implicit val accessTokenFormat: OFormat[AccessToken] = Json.using[Json.WithDefaultValues].format
}
