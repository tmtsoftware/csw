package csw.auth.adapters.akka.http
import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.auth.core.token.AccessToken
import org.mockito.Mockito.when
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.mockito.MockitoSugar

class CustomPolicyDirectiveTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("customPolicy directive should return AuthenticationFailedRejection when token is invalid") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`invalidTokenStr`) ⇒ None
      case _                           ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route =
      secure { implicit at ⇒
        get {
          customPolicy(at => at.given_name.contains("John")) {
            complete("OK")
          }
        }
      }

    Get("/test").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("customPolicy directive should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val authenticator: Authenticator[AccessToken] = _ ⇒ None

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = secure { implicit at ⇒
      get {
        customPolicy(_ => false) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("customPolicy directive should return AuthorizationFailedRejection when policy does not match") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val validTokenWithPolicyViolationStr    = "validTokenWithPolicyViolation"
    val validTokenWithPolicyViolationHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyViolationStr))

    val validTokenWithPolicyViolation = mock[AccessToken]

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithPolicyViolationStr`) ⇒ Some(validTokenWithPolicyViolation)
      case _                                            ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = secure { implicit at ⇒
      get {
        customPolicy(_ => false) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPolicyViolationHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("customPolicy directive should return 200 OK when policy matches") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val validTokenWithPolicyMatchStr    = "validTokenWithPolicyMatch"
    val validTokenWithPolicyMatchHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyMatchStr))

    val validTokenWithPolicyMatch = mock[AccessToken]

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithPolicyMatchStr`) ⇒ Some(validTokenWithPolicyMatch)
      case _                                        ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = secure { implicit at ⇒
      get {
        customPolicy(_ => true) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPolicyMatchHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("customPolicy directive overload should return AuthorizationFailedRejection when policy does not match") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val validTokenWithPolicyViolationStr    = "validTokenWithPolicyViolation"
    val validTokenWithPolicyViolationHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyViolationStr))

    val validTokenWithPolicyViolation = mock[AccessToken]

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithPolicyViolationStr`) ⇒ Some(validTokenWithPolicyViolation)
      case _                                            ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = secure { implicit at ⇒
      get {
        customPolicy(false) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPolicyViolationHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("customPolicy directive overload should return 200 OK when policy matches") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val validTokenWithPolicyMatchStr    = "validTokenWithPolicyMatch"
    val validTokenWithPolicyMatchHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyMatchStr))

    val validTokenWithPolicyMatch = mock[AccessToken]

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithPolicyMatchStr`) ⇒ Some(validTokenWithPolicyMatch)
      case _                                        ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = secure { implicit at ⇒
      get {
        customPolicy(policy = true) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPolicyMatchHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
