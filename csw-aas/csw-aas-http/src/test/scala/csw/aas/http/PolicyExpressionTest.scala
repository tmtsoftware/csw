package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.http.AuthorizationPolicy.{CustomPolicy, RealmRolePolicy}
import msocket.security.AccessControllerFactory
import msocket.security.api.TokenValidator
import msocket.security.models.AccessToken
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class PolicyExpressionTest extends AnyFunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  case class TestCase(left: Boolean, operator: String, right: Boolean, expectedOutcome: Boolean)

  val testCases = List(
    //AND truth table
    TestCase(true, "And", true, true),
    TestCase(true, "And", false, false),
    TestCase(false, "And", true, false),
    TestCase(false, "And", false, false),
    //OR truth table
    TestCase(true, "Or", true, true),
    TestCase(true, "Or", false, true),
    TestCase(false, "Or", true, true),
    TestCase(false, "Or", false, false)
  )

  testCases.foreach(testCase => {
    import testCase._
    test(s"$left $operator $right = $expectedOutcome") {
      val tokenValidator     = mock[TokenValidator]
      val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")

      val tokenStr = "token"
      val token    = mock[AccessToken]
      val header   = Authorization(OAuth2BearerToken(tokenStr))

      when(token.hasRealmRole("admin")).thenReturn(left)
      when(token.clientId).thenReturn(if (right) Some("abc") else None)

      when(tokenValidator.validate(tokenStr)).thenReturn(Future.successful(token))

      val policyExpression = operator match {
        case "And" => RealmRolePolicy("admin") & CustomPolicy(_.clientId.isDefined)
        case "Or"  => RealmRolePolicy("admin") | CustomPolicy(_.clientId.isDefined)
      }

      val route: Route = securityDirectives.secure(policyExpression) { at =>
        get {
          complete("OK")
        }
      }

      Get("/").addHeader(header) ~> route ~> check {
        if (expectedOutcome)
          status shouldBe StatusCodes.OK
        else
          rejection shouldBe a[AuthorizationFailedRejection]
      }
    }
  })

  test("policy expression should return AuthenticationFailedRejection when token is invalid | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")

    val tokenStr    = "token"
    val tokenHeader = Authorization(OAuth2BearerToken(tokenStr))

    when(tokenValidator.validate(tokenStr)).thenReturn(Future.failed(new RuntimeException("invalid")))

    val route: Route = securityDirectives.secure(RealmRolePolicy("eng") | RealmRolePolicy("admin")) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(tokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("policy expression policy should return AuthenticationFailedRejection when token is not present | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")

    val route: Route = securityDirectives.secure(RealmRolePolicy("admin") & CustomPolicy(_.clientId.isDefined)) { at =>
      get {
        complete("OK")
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("policy expression policy should return AuthorizationFailedRejection when expression resolves to false | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")

    val tokenStr = "token"
    val token    = mock[AccessToken]
    val header   = Authorization(OAuth2BearerToken(tokenStr))

    when(token.hasRealmRole("admin")).thenReturn(false)
    when(token.clientId).thenReturn(Some("abc"))

    when(tokenValidator.validate(tokenStr)).thenReturn(Future.successful(token))

    val route: Route = securityDirectives.secure(CustomPolicy(_.clientId.isDefined) & RealmRolePolicy("admin")) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(header) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("policy expression policy should return 200 OK when token is valid & policy expression resolves to true | DEOPSCSW-579") {
    val tokenValidator     = mock[TokenValidator]
    val securityDirectives = new SecurityDirectives(new AccessControllerFactory(tokenValidator, true), "TMT")
    val tokenStr           = "token"
    val token              = mock[AccessToken]
    val header             = Authorization(OAuth2BearerToken(tokenStr))

    when(token.hasRealmRole("admin")).thenReturn(true)
    when(token.hasRealmRole("eng")).thenReturn(false)

    when(tokenValidator.validate(tokenStr)).thenReturn(Future.successful(token))

    val route: Route = securityDirectives.secure(RealmRolePolicy("eng") | RealmRolePolicy("admin")) { at =>
      get {
        complete("OK")
      }
    }

    Get("/").addHeader(header) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
