package csw.config.server.mocks

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.Directives.AsyncAuthenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.AccessToken
import csw.aas.http.{Authentication, SecurityDirectives}
import csw.config.api.TokenFactory
import org.keycloak.adapters.KeycloakDeployment
import org.mockito.MockitoSugar

import scala.concurrent.Future

class JMockedAuthentication extends MockedAuthentication

trait MockedAuthentication extends MockitoSugar {
  val authentication: Authentication = mock[Authentication]

  private val keycloakDeployment = new KeycloakDeployment()
  keycloakDeployment.setRealm("TMT")
  keycloakDeployment.setResourceName("test")

  private val authConfig: AuthConfig = mock[AuthConfig]
  when(authConfig.getDeployment).thenReturn(keycloakDeployment)

  val securityDirectives = new SecurityDirectives(authentication, keycloakDeployment.getRealm, keycloakDeployment.getResourceName)

  val roleMissingTokenStr = "rolemissing"
  val validTokenStr       = "valid"
  val invalidTokenStr     = "invalid"

  val preferredUserName = "root"

  val roleMissingToken: AccessToken = mock[AccessToken]
  val validToken: AccessToken       = mock[AccessToken]
  val invalidToken: AccessToken     = mock[AccessToken]

  private val authenticator: AsyncAuthenticator[AccessToken] = {
    case Provided(`roleMissingTokenStr`) ⇒ Future.successful(Some(roleMissingToken))
    case Provided(`validTokenStr`)       ⇒ Future.successful(Some(validToken))
    case _                               ⇒ Future.successful(None)
  }
  when(roleMissingToken.hasResourceRole("admin", "test")).thenReturn(false)
  when(validToken.hasResourceRole("admin", "test")).thenReturn(true)
  when(validToken.preferred_username).thenReturn(Some(preferredUserName))
  when(validToken.userOrClientName).thenReturn(preferredUserName)
  when(authentication.authenticator).thenReturn(authenticator)

  val roleMissingTokenHeader = Authorization(OAuth2BearerToken(roleMissingTokenStr))
  val validTokenHeader       = Authorization(OAuth2BearerToken(validTokenStr))
  val invalidTokenHeader     = Authorization(OAuth2BearerToken(invalidTokenStr))

  val factory: TokenFactory = mock[TokenFactory]
  when(factory.getToken).thenReturn(validTokenStr)
}
