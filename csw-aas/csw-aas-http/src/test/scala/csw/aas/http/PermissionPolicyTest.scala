package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.PermissionPolicy
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class PermissionPolicyTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("permission policy should return AuthenticationFailedRejection when token is invalid") {
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
        securityDirectives.authorize(PermissionPolicy("read"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("permission policy should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val authenticator: AsyncAuthenticator[AccessToken] = _ ⇒ Future.successful(None)

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(PermissionPolicy("read"), at) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("permission policy should return AuthorizationFailedRejection when token does not have permission") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val validTokenWithoutPermissionStr    = "validTokenWithoutPermissionStr"
    val validTokenWithoutPermissionHeader = Authorization(OAuth2BearerToken(validTokenWithoutPermissionStr))

    val validTokenWithoutPermission = mock[AccessToken]

    when(validTokenWithoutPermission.hasPermission("read"))
      .thenReturn(false)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithoutPermissionStr`) ⇒ Future.successful(Some(validTokenWithoutPermission))
      case _                                          ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(PermissionPolicy("read"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithoutPermissionHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("permission policy should return 200 OK when token is valid & has permission") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val validTokenWithPermissionStr    = "validTokenWithPermissionStr"
    val validTokenWithPermissionHeader = Authorization(OAuth2BearerToken(validTokenWithPermissionStr))

    val validTokenWithPermission = mock[AccessToken]

    when(validTokenWithPermission.hasPermission(scope = "read", resource = "Default Resource"))
      .thenReturn(true)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithPermissionStr`) ⇒ Future.successful(Some(validTokenWithPermission))
      case _                                       ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(PermissionPolicy("read"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPermissionHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
