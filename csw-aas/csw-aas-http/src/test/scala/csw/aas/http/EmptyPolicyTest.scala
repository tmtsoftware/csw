package csw.aas.http

import akka.http.javadsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.http.AuthorizationPolicy.EmptyPolicy
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class EmptyPolicyTest extends AnyFunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("empty policy should return AuthenticationFailedRejection when token is invalid | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    when(tokenValidator.validate(invalidTokenStr)).thenReturn(Future.failed(new RuntimeException("invalid")))

    val route: Route = securityDirectives.secure(EmptyPolicy) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("empty policy should return AuthenticationFailedRejection when token is not present | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")

    val route: Route = securityDirectives.secure(EmptyPolicy) { at =>
      get {
        complete("OK")
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("empty policy should return 200 OK when token is valid | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")

    val validTokenStr    = "validTokenStr"
    val validTokenHeader = Authorization(OAuth2BearerToken(validTokenStr))

    val validToken = mock[AccessToken]

    when(tokenValidator.validate(validTokenStr)).thenReturn(Future.successful(validToken))

    val route: Route = securityDirectives.secure(EmptyPolicy) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(validTokenHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
