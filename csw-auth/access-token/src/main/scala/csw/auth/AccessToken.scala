package csw.auth

import csw.auth.TokenVerificationFailure.{InvalidToken, TokenExpired}
import csw.auth.token.claims.{Access, Audience, Authorization, Permission}
import org.keycloak.adapters.rotation.AdapterTokenVerifier
import org.keycloak.common.VerificationException
import org.keycloak.exceptions.TokenNotActiveException
import org.keycloak.representations.idm.authorization
import org.keycloak.representations.{AccessToken => KeycloakAccessToken}
import play.api.libs.json._

import scala.collection.JavaConverters._

//todo: integrate csw logging
case class AccessToken(
    //standard checks
    sub: Option[String],
    iat: Option[Long],
    exp: Option[Long],
    iss: Option[String],
    aud: Option[Audience],
    jti: Option[String],
    //additional information
    given_name: Option[String],
    family_name: Option[String],
    name: Option[String],
    preferred_username: Option[String],
    email: Option[String],
    scope: Option[String],
    //auth
    realm_access: Option[Access],
    resource_access: Option[Map[String, Access]],
    authorization: Option[Authorization]
) {
  def hasPermission(scope: String, resource: String): Boolean = {
    this.authorization match {
      case Some(Authorization(Some(perms))) =>
        perms.exists {
          case Permission(_, r, Some(scopes)) if r == resource => scopes.contains(scope)
          case _                                               => false
        }
      case _ => false
    }
  }

  def hasRole(role: String): Boolean = {
    val allRealmRoles: Set[String] = this.realm_access.flatMap(_.roles).getOrElse(Set.empty)
    val clientName: String         = Keycloak.deployment.getResourceName

    val maybeRoles = for {
      resourceAccesses ← this.resource_access
      resourceAccess   ← resourceAccesses.get(clientName)
      roles            ← resourceAccess.roles
    } yield roles

    val allResourceRoles: Set[String] = maybeRoles.getOrElse(Set.empty)

    (allRealmRoles ++ allResourceRoles).contains(role)
  }
}

//todo: think about splitting verification and decoding
object AccessToken {

  implicit val accessTokenFormat: OFormat[AccessToken] = Json.format[AccessToken]

  def verifyAndDecode(token: String): Either[TokenVerificationFailure, AccessToken] = {

    val keycloakToken: Either[TokenVerificationFailure, KeycloakAccessToken] = try {
      Right(AdapterTokenVerifier.verifyToken(token, Keycloak.deployment))
    } catch {
      case _: TokenNotActiveException => Left(TokenExpired)
      case ex: VerificationException  => Left(InvalidToken(ex.getMessage))
    }

    keycloakToken
      .map(convert)

  }

  private def convert(keycloakAccessToken: KeycloakAccessToken): AccessToken = {

    val keycloakPermissions: Set[authorization.Permission] =
      keycloakAccessToken.getAuthorization.getPermissions.asScala.toSet

    val permissions: Set[Permission] = keycloakPermissions.map(
      permission => Permission(permission.getResourceId, permission.getResourceName, Some(permission.getScopes.asScala.toSet))
    )

    //todo: remove var
    var resourceAccess: Map[String, Access] = Map.empty
    keycloakAccessToken.getResourceAccess.forEach(
      (key, access) => resourceAccess += (key -> Access(Some(access.getRoles.asScala.toSet)))
    )

    AccessToken(
      sub = Some(keycloakAccessToken.getSubject),
      iat = Some(keycloakAccessToken.getIssuedAt.toLong),
      exp = Some(keycloakAccessToken.getExpiration.toLong),
      iss = Some(keycloakAccessToken.getIssuer),
      aud = Some(Audience(keycloakAccessToken.getAudience.toSeq)),
      jti = Some(keycloakAccessToken.getId),
      given_name = Some(keycloakAccessToken.getGivenName),
      family_name = Some(keycloakAccessToken.getFamilyName),
      name = Some(keycloakAccessToken.getFamilyName),
      preferred_username = Some(keycloakAccessToken.getPreferredUsername),
      email = Some(keycloakAccessToken.getEmail),
      scope = Some(keycloakAccessToken.getScope),
      realm_access = Some(Access(Some(keycloakAccessToken.getRealmAccess.getRoles.asScala.toSet))),
      resource_access = Some(resourceAccess),
      authorization = Some(Authorization(Some(permissions)))
    )
  }

}
