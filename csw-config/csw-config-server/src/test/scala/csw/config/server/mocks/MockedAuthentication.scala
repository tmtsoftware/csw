package csw.config.server.mocks

import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import csw.aas.core.deployment.AuthConfig
import csw.aas.http.SecurityDirectives
import csw.config.api.TokenFactory
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken
import org.keycloak.adapters.KeycloakDeployment
import org.mockito.MockitoSugar

import scala.concurrent.Future

class JMockedAuthentication extends MockedAuthentication

trait MockedAuthentication {
  import MockitoSugar._

  private val keycloakDeployment = new KeycloakDeployment()
  keycloakDeployment.setRealm("TMT")
  keycloakDeployment.setResourceName("tmt-backend-app")

  private val authConfig: AuthConfig = mock[AuthConfig]
  when(authConfig.getDeployment).thenReturn(keycloakDeployment)

  val roleMissingTokenStr = "rolemissing"
  val validTokenStr       = "validToken"
  val invalidTokenStr     = "invalid"

  val preferredUserName = "root"

  val roleMissingToken: AccessToken = mock[AccessToken]
  val validToken: AccessToken       = mock[AccessToken]
  val invalidToken: AccessToken     = mock[AccessToken]

  val tokenValidator: TokenValidator = {
    case `roleMissingTokenStr` => Future.successful(roleMissingToken)
    case `validTokenStr`       => Future.successful(validToken)
    case token                 => Future.failed(new RuntimeException(s"unexpected token $token"))
  }

  val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), keycloakDeployment.getRealm)

  when(roleMissingToken.hasRealmRole("config-admin")).thenReturn(false)
  when(validToken.hasRealmRole("config-admin")).thenReturn(true)
  when(validToken.preferred_username).thenReturn(Some(preferredUserName))
  when(validToken.userOrClientName).thenReturn(preferredUserName)

  val roleMissingTokenHeader = Authorization(OAuth2BearerToken(roleMissingTokenStr))
  val validTokenHeader       = Authorization(OAuth2BearerToken(validTokenStr))
  val invalidTokenHeader     = Authorization(OAuth2BearerToken(invalidTokenStr))

  val factory: TokenFactory = mock[TokenFactory]
  when(factory.getToken).thenReturn(validTokenStr)
}
