package csw.aas.http

import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.headers.{HttpChallenges, OAuth2BearerToken}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives.{extractCredentials, extractRequestContext, method, onSuccess}
import akka.http.scaladsl.server.directives.BasicDirectives.provide
import akka.http.scaladsl.server.directives.RouteDirectives.reject
import akka.http.scaladsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection, Directive1}
import msocket.security.api.AuthorizationPolicy
import msocket.security.models.{AccessStatus, AccessToken}
import msocket.security.{AccessControllerFactory, models}

private[http] class PolicyValidator(accessControllerFactory: AccessControllerFactory, realm: String) {

  /**
   * Rejects all un-authorized requests
   *
   * @param authorizationPolicy Authorization policy to use for filtering requests.
   *                            There are different types of authorization policies. See [[csw.aas.http.AuthorizationPolicy]]
   */
  private def validatePolicy(authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] = {
    extractStringToken.flatMap { maybeToken =>
      extractRequestContext.flatMap { rc =>
        import rc.executionContext
        val accessController     = accessControllerFactory.make(maybeToken)
        val eventualAccessStatus = accessController.authenticateAndAuthorize(Some(authorizationPolicy))
        onSuccess(eventualAccessStatus).flatMap(getAccessToken)
      }
    }
  }

  def validate(httpMethod: HttpMethod, authorizationPolicy: AuthorizationPolicy): Directive1[AccessToken] =
    method(httpMethod) & validatePolicy(authorizationPolicy)

  private def extractStringToken: Directive1[Option[String]] = {
    extractCredentials.map {
      case Some(OAuth2BearerToken(token)) => Some(token)
      case _                              => None
    }
  }

  private def getAccessToken(accessStatus: AccessStatus): Directive1[AccessToken] = {
    lazy val challenge = HttpChallenges.oAuth2(realm)
    accessStatus match {
      case models.AccessStatus.Authorized(accessToken) => provide(accessToken)
      case models.AccessStatus.TokenMissing()          => reject(AuthenticationFailedRejection(CredentialsMissing, challenge))
      case models.AccessStatus.AuthenticationFailed(_) => reject(AuthenticationFailedRejection(CredentialsRejected, challenge))
      case models.AccessStatus.AuthorizationFailed(_)  => reject(AuthorizationFailedRejection)
    }
  }
}
