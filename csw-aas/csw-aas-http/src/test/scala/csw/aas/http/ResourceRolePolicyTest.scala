package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.ResourceRolePolicy
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class ResourceRolePolicyTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("resourceRole policy should return AuthenticationFailedRejection when token is invalid") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`invalidTokenStr`) ⇒ Future.successful(None)
      case _                           ⇒ Future.successful(None)
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
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val authenticator: AsyncAuthenticator[AccessToken] = _ ⇒ Future.successful(None)

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
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val validTokenWithoutResourceRoleStr = "validTokenWithoutResourceRoleStr"

    val validTokenWithoutResourceRole = mock[AccessToken]

    val validTokenWithoutResourceRoleHeader = Authorization(OAuth2BearerToken(validTokenWithoutResourceRoleStr))

    when(validTokenWithoutResourceRole.hasResourceRole("admin", "test"))
      .thenReturn(false)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithoutResourceRoleStr`) ⇒ Future.successful(Some(validTokenWithoutResourceRole))
      case _                                            ⇒ Future.successful(None)
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
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val validTokenWithResourceRoleStr    = "validTokenWithResourceRoleStr"
    val validTokenWithResourceRole       = mock[AccessToken]
    val validTokenWithResourceRoleHeader = Authorization(OAuth2BearerToken(validTokenWithResourceRoleStr))
    when(validTokenWithResourceRole.hasResourceRole("admin", "test"))
      .thenReturn(true)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithResourceRoleStr`) ⇒ Future.successful(Some(validTokenWithResourceRole))
      case _                                         ⇒ Future.successful(None)
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
