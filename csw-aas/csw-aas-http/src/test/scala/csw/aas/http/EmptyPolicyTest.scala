package csw.aas.http

import akka.http.javadsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.EmptyPolicy
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class EmptyPolicyTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("empty policy should return AuthenticationFailedRejection when token is invalid") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`invalidTokenStr`) ⇒ None
      case _                           ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(EmptyPolicy, at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("empty policy should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)

    val authenticator: Authenticator[AccessToken] = _ ⇒ None

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(EmptyPolicy, at) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("empty policy should return 200 OK when token is valid & has permission") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)

    val validTokenWithPermissionStr    = "validTokenWithPermissionStr"
    val validTokenWithPermissionHeader = Authorization(OAuth2BearerToken(validTokenWithPermissionStr))

    val validTokenWithPermission = mock[AccessToken]

    when(validTokenWithPermission.hasPermission(scope = "read", resource = "Default Resource"))
      .thenReturn(true)

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithPermissionStr`) ⇒ Some(validTokenWithPermission)
      case _                                       ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(EmptyPolicy, at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPermissionHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
