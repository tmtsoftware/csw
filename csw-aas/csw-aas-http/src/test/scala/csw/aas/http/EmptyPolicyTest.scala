package csw.aas.http

import akka.http.javadsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.EmptyPolicy
import org.mockito.MockitoSugar
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class EmptyPolicyTest extends AnyFunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("empty policy should return AuthenticationFailedRejection when token is invalid | DEOPSCSW-579") {
    val authentication: Authentication         = mock[Authentication]
    val securityDirectives: SecurityDirectives = new SecurityDirectives(authentication, "TMT", false)
    //new SecurityDirectives(authentication, authConfig)

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`invalidTokenStr`) => Future.successful(None)
      case _                           => Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at =>
      get {
        securityDirectives.authorize(EmptyPolicy, at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("empty policy should return AuthenticationFailedRejection when token is not present | DEOPSCSW-579") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", false)

    val authenticator: AsyncAuthenticator[AccessToken] = _ => Future.successful(None)

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at =>
      get {
        securityDirectives.authorize(EmptyPolicy, at) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("empty policy should return 200 OK when token is valid | DEOPSCSW-579") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", false)

    val validTokenStr    = "validTokenStr"
    val validTokenHeader = Authorization(OAuth2BearerToken(validTokenStr))

    val validToken = mock[AccessToken]

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenStr`) => Future.successful(Some(validToken))
      case _                         => Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at =>
      get {
        securityDirectives.authorize(EmptyPolicy, at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
