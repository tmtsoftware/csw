package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.AuthorizationPolicy.CustomPolicy
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class CustomPolicyTest extends AnyFunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("custom policy should return AuthenticationFailedRejection when token is invalid | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    when(tokenValidator.validate(invalidTokenStr)).thenReturn(Future.failed(new RuntimeException("invalid")))

    val route: Route =
      policyValidator.validate(GET, CustomPolicy(token => token.given_name.contains("John"))) { at =>
        get {
          complete("OK")
        }
      }

    Get("/test").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("custom policy should return AuthenticationFailedRejection when token is not present | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val route: Route = policyValidator.validate(GET, CustomPolicy(_ => false)) { at =>
      get {
        complete("OK")
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("custom policy should return AuthorizationFailedRejection when policy does not match | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val validTokenWithPolicyViolationStr    = "validTokenWithPolicyViolation"
    val validTokenWithPolicyViolationHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyViolationStr))

    val validTokenWithPolicyViolation = mock[AccessToken]

    when(tokenValidator.validate(validTokenWithPolicyViolationStr)).thenReturn(Future.successful(validTokenWithPolicyViolation))

    val route: Route = policyValidator.validate(GET, CustomPolicy(_ => false)) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(validTokenWithPolicyViolationHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("custom policy should return 200 OK when policy matches | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val validTokenWithPolicyMatchStr    = "validTokenWithPolicyMatch"
    val validTokenWithPolicyMatchHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyMatchStr))

    val validTokenWithPolicyMatch = mock[AccessToken]

    when(tokenValidator.validate(validTokenWithPolicyMatchStr)).thenReturn(Future.successful(validTokenWithPolicyMatch))

    val route: Route = policyValidator.validate(GET, CustomPolicy(_ => true)) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(validTokenWithPolicyMatchHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
