package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.CustomPolicy
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class CustomPolicyTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("custom policy should return AuthenticationFailedRejection when token is invalid") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`invalidTokenStr`) ⇒ Future.successful(None)
      case _                           ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route =
      securityDirectives.authenticate { implicit at ⇒
        get {
          securityDirectives.authorize(CustomPolicy(token => token.given_name.contains("John")), at) {
            complete("OK")
          }
        }
      }

    Get("/test").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("custom policy should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val authenticator: AsyncAuthenticator[AccessToken] = _ ⇒ Future.successful(None)

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(CustomPolicy(_ => false), at) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("custom policy should return AuthorizationFailedRejection when policy does not match") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val validTokenWithPolicyViolationStr    = "validTokenWithPolicyViolation"
    val validTokenWithPolicyViolationHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyViolationStr))

    val validTokenWithPolicyViolation = mock[AccessToken]

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithPolicyViolationStr`) ⇒ Future.successful(Some(validTokenWithPolicyViolation))
      case _                                            ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(CustomPolicy(_ => false), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPolicyViolationHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("custom policy should return 200 OK when policy matches") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val validTokenWithPolicyMatchStr    = "validTokenWithPolicyMatch"
    val validTokenWithPolicyMatchHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyMatchStr))

    val validTokenWithPolicyMatch = mock[AccessToken]

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithPolicyMatchStr`) ⇒ Future.successful(Some(validTokenWithPolicyMatch))
      case _                                        ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(CustomPolicy(_ => true), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPolicyMatchHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
