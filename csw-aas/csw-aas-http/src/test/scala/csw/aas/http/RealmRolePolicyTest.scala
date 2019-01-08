package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class RealmRolePolicyTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("realmRole policy should return AuthenticationFailedRejection when token is invalid") {
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
        securityDirectives.authorize(RealmRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("realmRole policy should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val authenticator: AsyncAuthenticator[AccessToken] = _ ⇒ Future.successful(None)

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(RealmRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("realmRole policy should return AuthorizationFailedRejection when token does not have realmRole") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val validTokenWithoutRealmRoleStr = "validTokenWithoutRealmRoleStr"

    val validTokenWithoutRealmRole = mock[AccessToken]

    val validTokenWithoutRealmRoleHeader = Authorization(OAuth2BearerToken(validTokenWithoutRealmRoleStr))

    when(validTokenWithoutRealmRole.hasRealmRole("admin"))
      .thenReturn(false)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithoutRealmRoleStr`) ⇒ Future.successful(Some(validTokenWithoutRealmRole))
      case _                                         ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(RealmRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithoutRealmRoleHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("realmRole policy should return 200 OK when token is valid & has realmRole") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val validTokenWithRealmRoleStr    = "validTokenWithRealmRoleStr"
    val validTokenWithRealmRole       = mock[AccessToken]
    val validTokenWithRealmRoleHeader = Authorization(OAuth2BearerToken(validTokenWithRealmRoleStr))
    when(validTokenWithRealmRole.hasRealmRole("admin"))
      .thenReturn(true)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithRealmRoleStr`) ⇒ Future.successful(Some(validTokenWithRealmRole))
      case _                                      ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(RealmRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithRealmRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
