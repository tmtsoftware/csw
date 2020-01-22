package csw.aas.http

import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.{RealmRolePolicy, SubsystemPolicy}
import csw.prefix.models.Subsystem.{CSW, ESW}
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class SubsystemPolicyTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("subsystem policy should return AuthenticationFailedRejection when token is invalid") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test", false)

    val invalidTokenStr    = "invalid"
    val invalidTokenHeader = Authorization(OAuth2BearerToken(invalidTokenStr))

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`invalidTokenStr`) => Future.successful(None)
      case _                           => Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at =>
      get {
        securityDirectives.authorize(SubsystemPolicy(CSW), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("subsystem policy should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test", false)

    val authenticator: AsyncAuthenticator[AccessToken] = _ => Future.successful(None)

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at =>
      get {
        securityDirectives.authorize(SubsystemPolicy(ESW), at) {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("subsystem policy should return AuthorizationFailedRejection when token does not have subsystem") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test", false)

    val validTokenWithoutSubsystemStr = "validTokenWithoutSubsystemStr"

    val validTokenWithoutSubsystemRole = mock[AccessToken]

    val validTokenWithoutSubsystemHeader = Authorization(OAuth2BearerToken(validTokenWithoutSubsystemStr))

    when(validTokenWithoutSubsystemRole.hasSubsystem(CSW))
      .thenReturn(false)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithoutSubsystemStr`) => Future.successful(Some(validTokenWithoutSubsystemRole))
      case _                                         => Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at =>
      get {
        securityDirectives.authorize(SubsystemPolicy(CSW), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithoutSubsystemHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("subsystem policy should return 200 OK when token is valid & has subsystem") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test", false)

    val validTokenWithSubsystemStr    = "validTokenWithSubsystemStr"
    val validTokenWithSubsystem       = mock[AccessToken]
    val validTokenWithSubsystemHeader = Authorization(OAuth2BearerToken(validTokenWithSubsystemStr))
    when(validTokenWithSubsystem.hasSubsystem(CSW))
      .thenReturn(true)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithSubsystemStr`) => Future.successful(Some(validTokenWithSubsystem))
      case _                                      => Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = securityDirectives.authenticate { implicit at =>
      get {
        securityDirectives.authorize(SubsystemPolicy(CSW), at) {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithSubsystemHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
