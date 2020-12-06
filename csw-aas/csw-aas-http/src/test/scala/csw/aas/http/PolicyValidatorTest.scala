package csw.aas.http

import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, HttpChallenge, OAuth2BearerToken}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection, Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.http.AuthorizationPolicy.{CustomPolicy, RealmRolePolicy}
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class PolicyValidatorTest extends AnyFunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("validate using customPolicy should return 200 OK when policy matches | DEOPSCSW-579, CSW-98") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val validTokenWithPolicyMatchStr    = "validTokenWithPolicyMatch"
    val validTokenWithPolicyMatchHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyMatchStr))

    val validTokenWithPolicyMatch = mock[AccessToken]

    when(tokenValidator.validate(validTokenWithPolicyMatchStr)).thenReturn(Future.successful(validTokenWithPolicyMatch))

    val route: Route = post {
      policyValidator.validate(POST, CustomPolicy(_ => true)) { _ => complete("OK") }
    }

    Post("/").addHeader(validTokenWithPolicyMatchHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("validate using customPolicy should return 200 OK when token not passed and auth is disabled | DEOPSCSW-579") {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, false), "TMT")

    val route: Route = post {
      policyValidator.validate(POST, CustomPolicy(_ => true)) { _ => complete("OK") }
    }

    Post("/") ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test(
    "validate should throw AuthenticationFailedRejection - CredentialsMissing when token not passed | CSW-98"
  ) {
    val tokenValidator  = mock[TokenValidator]
    val policyValidator = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")

    val route: Route = post {
      policyValidator.validate(POST, CustomPolicy(_ => true)) { _ => complete("OK") }
    }

    Post("/") ~> route ~> check {
      rejection shouldEqual AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", "TMT"))
    }
  }

  test(
    "validate should throw AuthenticationFailedRejection - CredentialsRejected when invalid token passed | CSW-98"
  ) {
    val tokenValidator     = mock[TokenValidator]
    val policyValidator    = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")
    val invalidTokenStr    = "invalidToken"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    when(tokenValidator.validate(invalidTokenStr)).thenReturn(Future.failed(new RuntimeException("invalid token passed")))

    val route: Route = post {
      policyValidator.validate(POST, CustomPolicy(_ => true)) { _ => complete("OK") }
    }

    Post("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldEqual AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "TMT"))
    }
  }

  test(
    "validate should throw AuthorizationFailedRejection when token without valid role is passed | CSW-98"
  ) {
    val tokenValidator         = mock[TokenValidator]
    val policyValidator        = new PolicyValidator(new AccessControllerFactory(tokenValidator, true), "TMT")
    val tokenWithoutRoleStr    = "validToken"
    val tokenWithoutRoleHeader = Authorization(OAuth2BearerToken(tokenWithoutRoleStr))

    val tokenWithoutRole = mock[AccessToken]
    when(tokenWithoutRole.hasRealmRole(any[String])).thenReturn(false)
    when(tokenValidator.validate(tokenWithoutRoleStr)).thenReturn(Future.successful(tokenWithoutRole))

    val route: Route = post {
      policyValidator.validate(POST, RealmRolePolicy("some-role")) { _ => complete("OK") }
    }

    Post("/").addHeader(tokenWithoutRoleHeader) ~> route ~> check {
      rejection shouldEqual AuthorizationFailedRejection
    }
  }

}
