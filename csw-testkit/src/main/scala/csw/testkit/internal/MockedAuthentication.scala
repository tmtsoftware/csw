package csw.testkit.internal

import csw.aas.core.deployment.AuthConfig
import csw.aas.http.{PolicyValidator, SecurityDirectives}
import csw.config.api.TokenFactory
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken
import org.keycloak.adapters.KeycloakDeployment
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

private[testkit] trait MockedAuthentication extends MockitoSugar {
  private val keycloakDeployment = new KeycloakDeployment()
  keycloakDeployment.setRealm("TMT")
  keycloakDeployment.setResourceName("tmt-backend-app")

  private val authConfig: AuthConfig = mock[AuthConfig]
  when(authConfig.getDeployment).thenReturn(keycloakDeployment)

  private val validTokenStr           = "validToken"
  private val validToken: AccessToken = mock[AccessToken]

  val tokenValidator: TokenValidator = {
    case `validTokenStr` => Future.successful(validToken)
    case token           => Future.failed(new RuntimeException(s"unexpected token $token"))
  }
  val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), keycloakDeployment.getRealm)
  val _securityDirectives =
    new SecurityDirectives(policyValidator)

  when(validToken.hasRealmRole("config-admin")).thenReturn(true)
  when(validToken.preferred_username).thenReturn(Some("test"))
  when(validToken.userOrClientName).thenReturn("test")

  private val AdminRole = "location-admin"
  when(validToken.hasRealmRole(AdminRole)).thenReturn(true)

  private val factory: TokenFactory = mock[TokenFactory]
  when(factory.getToken).thenReturn(validTokenStr)
}
