package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.PolicyExpression.{And, ExpressionOperator, Or}
import csw.aas.http.AuthorizationPolicy.{ClientRolePolicy, CustomPolicy, RealmRolePolicy}
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class PolicyExpressionTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  case class TestCase(left: Boolean, operator: ExpressionOperator, right: Boolean, expectedOutcome: Boolean)

  val testCases = List(
    //AND truth table
    TestCase(true, And, true, true),
    TestCase(true, And, false, false),
    TestCase(false, And, true, false),
    TestCase(false, And, false, false),
    //OR truth table
    TestCase(true, Or, true, true),
    TestCase(true, Or, false, true),
    TestCase(false, Or, true, true),
    TestCase(false, Or, false, false),
  )

  testCases.foreach(testCase => {
    import testCase._
    test(s"$left $operator $right = $expectedOutcome") {
      val authentication: Authentication = mock[Authentication]
      val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

      val tokenStr = "token"
      val token    = mock[AccessToken]
      val header   = Authorization(OAuth2BearerToken(tokenStr))

      when(token.hasRealmRole("admin")).thenReturn(left)
      when(token.clientId).thenReturn(if (right) Some("abc") else None)

      val authenticator: AsyncAuthenticator[AccessToken] = {
        case Provided(_) ⇒ Future.successful(Some(token))
        case _           ⇒ Future.successful(None)
      }

      when(authentication.authenticator).thenReturn(authenticator)

      val policyExpression = operator match {
        case And => RealmRolePolicy("admin") & CustomPolicy(_.clientId.isDefined)
        case Or  => RealmRolePolicy("admin") | CustomPolicy(_.clientId.isDefined)
      }

      val route: Route = securityDirectives.authenticate { implicit at ⇒
        get {
          securityDirectives.authorize(policyExpression, at) {
            complete("OK")
          }
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

  test("policy expression should return AuthenticationFailedRejection when token is invalid") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val tokenStr    = "token"
    val tokenHeader = Authorization(OAuth2BearerToken(tokenStr))

    val authenticator: AsyncAuthenticator[AccessToken] = _ ⇒ Future.successful(None)

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(RealmRolePolicy("admin") | ClientRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(tokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("policy expression policy should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication                 = mock[Authentication]
    val securityDirectives                             = new SecurityDirectives(authentication, "TMT", "test")
    val authenticator: AsyncAuthenticator[AccessToken] = _ ⇒ Future.successful(None)

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(RealmRolePolicy("admin") & CustomPolicy(_.clientId.isDefined), at) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("policy expression policy should return AuthorizationFailedRejection when expression resolves to false") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")

    val tokenStr = "token"
    val token    = mock[AccessToken]
    val header   = Authorization(OAuth2BearerToken(tokenStr))

    when(token.hasRealmRole("admin")).thenReturn(false)
    when(token.clientId).thenReturn(Some("abc"))

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`tokenStr`) ⇒ Future.successful(Some(token))
      case _                    ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(CustomPolicy(_.clientId.isDefined) & RealmRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(header) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("policy expression policy should return 200 OK when token is valid & policy expression resolves to true") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")
    val tokenStr                       = "token"
    val token                          = mock[AccessToken]
    val header                         = Authorization(OAuth2BearerToken(tokenStr))

    when(token.hasRealmRole("admin"))
      .thenReturn(true)

    when(token.hasClientRole("admin", "test")).thenReturn(false)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`tokenStr`) ⇒ Future.successful(Some(token))
      case _                    ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at ⇒
      get {
        securityDirectives.authorize(ClientRolePolicy("admin") | RealmRolePolicy("admin"), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(header) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
