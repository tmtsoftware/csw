package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.http.AuthorizationPolicy.RealmRolePolicy
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class RealmRolePolicyTest extends AnyFunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("realmRole policy should return AuthenticationFailedRejection when token is invalid | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    when(tokenValidator.validate(invalidTokenStr)).thenReturn(Future.failed(new RuntimeException("invalid")))

    val route: Route = policyValidator.validate(GET, RealmRolePolicy("admin")) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("realmRole policy should return AuthenticationFailedRejection when token is not present | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val route: Route = policyValidator.validate(GET, RealmRolePolicy("admin")) { at =>
      get {
        complete("OK")
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("realmRole policy should return AuthorizationFailedRejection when token does not have realmRole | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val validTokenWithoutRealmRoleStr    = "validTokenWithoutRealmRoleStr"
    val validTokenWithoutRealmRoleHeader = Authorization(OAuth2BearerToken(validTokenWithoutRealmRoleStr))
    val validTokenWithoutRealmRole       = mock[AccessToken]

    when(validTokenWithoutRealmRole.hasRealmRole("admin")).thenReturn(false)

    when(tokenValidator.validate(validTokenWithoutRealmRoleStr)).thenReturn(Future.successful(validTokenWithoutRealmRole))

    val route: Route = policyValidator.validate(GET, RealmRolePolicy("admin")) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(validTokenWithoutRealmRoleHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("realmRole policy should return 200 OK when token is valid & has realmRole | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val validTokenWithRealmRoleStr    = "validTokenWithRealmRoleStr"
    val validTokenWithRealmRole       = mock[AccessToken]
    val validTokenWithRealmRoleHeader = Authorization(OAuth2BearerToken(validTokenWithRealmRoleStr))
    when(validTokenWithRealmRole.hasRealmRole("admin")).thenReturn(true)

    when(tokenValidator.validate(validTokenWithRealmRoleStr)).thenReturn(Future.successful(validTokenWithRealmRole))

    val route: Route = policyValidator.validate(GET, RealmRolePolicy("admin")) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(validTokenWithRealmRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
