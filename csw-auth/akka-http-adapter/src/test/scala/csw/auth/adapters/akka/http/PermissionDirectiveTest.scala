package csw.auth.adapters.akka.http
import akka.http.javadsl.server.{AuthenticationFailedRejection, AuthorizationFailedRejection}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit._
import csw.auth.core.token.AccessToken
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class PermissionDirectiveTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("permission directive should return AuthenticationFailedRejection when token is invalid") {
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

    val route: Route = secure { implicit at ⇒
      get {
        permission("read") {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(invalidTokenHeader) ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("permission directive should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val authenticator: Authenticator[AccessToken] = _ ⇒ None

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = secure { implicit at ⇒
      get {
        permission("read") {
          complete("OK")
        }
      }
    }

    Get("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }

  test("permission directive should return AuthorizationFailedRejection when token does not have permission") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val validTokenWithoutPermissionStr    = "validTokenWithoutPermissionStr"
    val validTokenWithoutPermissionHeader = Authorization(OAuth2BearerToken(validTokenWithoutPermissionStr))

    val validTokenWithoutPermission = mock[AccessToken]

    when(validTokenWithoutPermission.hasPermission("read", "Default Resource"))
      .thenReturn(false)

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithoutPermissionStr`) ⇒ Some(validTokenWithoutPermission)
      case _                                          ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = secure { implicit at ⇒
      get {
        permission("read") {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithoutPermissionHeader) ~> route ~> check {
      rejection shouldBe a[AuthorizationFailedRejection]
    }
  }

  test("permission directive should return 200 OK when token is valid & has permission") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication)
    import securityDirectives._

    val validTokenWithPermissionStr    = "validTokenWithPermissionStr"
    val validTokenWithPermissionHeader = Authorization(OAuth2BearerToken(validTokenWithPermissionStr))

    val validTokenWithPermission = mock[AccessToken]

    when(validTokenWithPermission.hasPermission("read", "Default Resource"))
      .thenReturn(true)

    val authenticator: Authenticator[AccessToken] = {
      case Provided(`validTokenWithPermissionStr`) ⇒ Some(validTokenWithPermission)
      case _                                       ⇒ None
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = secure { implicit at ⇒
      get {
        permission("read") {
          complete("OK")
        }
      }
    }

    Get("/").addHeader(validTokenWithPermissionHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }
}
