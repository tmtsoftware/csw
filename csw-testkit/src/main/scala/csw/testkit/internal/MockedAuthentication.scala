package csw.testkit.internal

import akka.http.scaladsl.server.Directives.Authenticator
import akka.http.scaladsl.server.directives.Credentials.Provided
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.AccessToken
import csw.aas.http.{Authentication, SecurityDirectives}
import csw.config.api.TokenFactory
import org.keycloak.adapters.KeycloakDeployment
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar

private[testkit] trait MockedAuthentication extends MockitoSugar {
  private val authentication: Authentication = mock[Authentication]

  private val keycloakDeployment = new KeycloakDeployment()
  keycloakDeployment.setRealm("TMT")
  keycloakDeployment.setResourceName("test")

  private val authConfig: AuthConfig = mock[AuthConfig]
  when(authConfig.getDeployment).thenReturn(keycloakDeployment)

  val _securityDirectives =
    new SecurityDirectives(authentication, keycloakDeployment.getRealm, keycloakDeployment.getResourceName)

  private val validTokenStr           = "valid"
  private val validToken: AccessToken = mock[AccessToken]

  private val authenticator: Authenticator[AccessToken] = {
    case Provided(`validTokenStr`) ⇒ Some(validToken)
    case _                         ⇒ None
  }

  when(validToken.hasResourceRole("admin", "test")).thenReturn(true)
  when(validToken.preferred_username).thenReturn(Some("test"))
  when(validToken.userOrClientName).thenReturn("test")
  when(authentication.authenticator).thenReturn(authenticator)

  private val factory: TokenFactory = mock[TokenFactory]
  when(factory.getToken).thenReturn(validTokenStr)
}
