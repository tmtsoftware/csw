package csw.auth

import csw.auth.TokenVerificationFailure.{InvalidToken, TokenExpired}
import csw.auth.commons.AuthLogger
import csw.auth.token.claims.{Access, Audience, Authorization, Permission}
import csw.logging.scaladsl.Logger
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

  private val log: Logger = AuthLogger.getLogger

  implicit val accessTokenFormat: OFormat[AccessToken] = Json.format[AccessToken]

  def verifyAndDecode(token: String): Either[TokenVerificationFailure, AccessToken] = {

    log.info("Verifying and decoding token")

    val keycloakToken: Either[TokenVerificationFailure, KeycloakAccessToken] = try {
      Right(AdapterTokenVerifier.verifyToken(token, Keycloak.deployment))
    } catch {
      case ex: TokenNotActiveException =>
        log.error(s"Token is expired with error ${ex.getMessage}")
        Left(TokenExpired)
      case ex: VerificationException =>
        log.error(ex.getMessage)
        Left(InvalidToken(ex.getMessage))
    }

    keycloakToken
      .map(convert)

  }

  private def convert(keycloakAccessToken: KeycloakAccessToken): AccessToken = {

    val keycloakPermissions: Option[Set[authorization.Permission]] =
      Option(keycloakAccessToken.getAuthorization)
        .flatMap(
          authorization => {
            Option(authorization.getPermissions).map(_.asScala).map(_.toSet)
          }
        )

    //todo: Remove var
    var resourceAccess: Map[String, Access] = Map.empty

    Option(keycloakAccessToken.getResourceAccess).foreach(
      ra =>
        ra.forEach(
          (key, access) => resourceAccess += key -> Access(Option(access.getRoles).map(_.asScala).map(_.toSet))
      )
    )

    val realmRoles = Option(keycloakAccessToken.getRealmAccess).map(_.getRoles).map(_.asScala).map(_.toSet)

    val accessToken = AccessToken(
      sub = Option(keycloakAccessToken.getSubject),
      iat = Option(keycloakAccessToken.getIssuedAt).map(_.toLong),
      exp = Option(keycloakAccessToken.getExpiration).map(_.toLong),
      iss = Option(keycloakAccessToken.getIssuer),
      aud = Option(keycloakAccessToken.getAudience).map(a ⇒ Audience(a.toSeq)),
      jti = Option(keycloakAccessToken.getId),
      given_name = Option(keycloakAccessToken.getGivenName),
      family_name = Option(keycloakAccessToken.getFamilyName),
      name = Option(keycloakAccessToken.getFamilyName),
      preferred_username = Option(keycloakAccessToken.getPreferredUsername),
      email = Option(keycloakAccessToken.getEmail),
      scope = Option(keycloakAccessToken.getScope),
      realm_access = Option(Access(realmRoles)),
      resource_access = Option(resourceAccess),
      authorization = Option(Authorization(keycloakPermissions.map(getPermissions)))
    )

    log.info("Verified and decoded token")

    accessToken
  }

  private def getPermissions(kpermissions: Set[authorization.Permission]): Set[Permission] = {
    kpermissions.map(
      permission =>
        Permission(
          permission.getResourceId,
          permission.getResourceName,
          Option(permission.getScopes).map(_.asScala).map(_.toSet)
      )
    )
  }

}
