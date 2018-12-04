package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.core.token.AccessToken
import AuthorizationPolicy.ResourceRolePolicy
import csw.aas.core.deployment.AuthConfig
import org.keycloak.adapters.KeycloakDeployment
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class ResourceRolePolicyTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  private val authConfig                             = AuthConfig.loadFromAppConfig
  private val keycloakDeployment: KeycloakDeployment = authConfig.getDeployment

  test("resourceRole policy should return AuthenticationFailedRejection when token is invalid") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, authConfig)

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`invalidTokenStr`) ⇒ None
      case _                           ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(ResourceRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("resourceRole policy should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, authConfig)

    val authenticator: Authenticator[AccessToken] = _ ⇒ None

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(ResourceRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("resourceRole policy should return AuthorizationFailedRejection when token does not have resourceRole") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, authConfig)

    val validTokenWithoutResourceRoleStr = "validTokenWithoutResourceRoleStr"

    val validTokenWithoutResourceRole = mock[AccessToken]

    val validTokenWithoutResourceRoleHeader = Authorization(OAuth2BearerToken(validTokenWithoutResourceRoleStr))

    when(validTokenWithoutResourceRole.hasResourceRole("admin", keycloakDeployment.getResourceName))
      .thenReturn(false)

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithoutResourceRoleStr`) ⇒ Some(validTokenWithoutResourceRole)
      case _                                            ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(ResourceRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithoutResourceRoleHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("resourceRole policy should return 200 OK when token is valid & has resourceRole") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, authConfig)

    val validTokenWithResourceRoleStr    = "validTokenWithResourceRoleStr"
    val validTokenWithResourceRole       = mock[AccessToken]
    val validTokenWithResourceRoleHeader = Authorization(OAuth2BearerToken(validTokenWithResourceRoleStr))
    when(validTokenWithResourceRole.hasResourceRole("admin", keycloakDeployment.getResourceName))
      .thenReturn(true)

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithResourceRoleStr`) ⇒ Some(validTokenWithResourceRole)
      case _                                         ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(ResourceRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithResourceRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
