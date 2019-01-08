package csw.aas.http

import akka.http.javadsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.server.directives.Credentials.Provided
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import csw.aas.core.token.AccessToken
import csw.aas.http.AuthorizationPolicy.{CustomPolicy, PermissionPolicy, RealmRolePolicy, ResourceRolePolicy}
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class SecurityDirectivesTest extends FunSuite with MockitoSugar with Directives with ScalatestRouteTest with Matchers {

  test("sGet using customPolicy should return 200 OK when policy matches") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")
    import securityDirectives._

    val validTokenWithPolicyMatchStr    = "validTokenWithPolicyMatch"
    val validTokenWithPolicyMatchHeader = Authorization(OAuth2BearerToken(validTokenWithPolicyMatchStr))

    val validTokenWithPolicyMatch = mock[AccessToken]

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithPolicyMatchStr`) ⇒ Future.successful(Some(validTokenWithPolicyMatch))
      case _                                        ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = sGet(CustomPolicy(_ ⇒ true)) { _ ⇒
      complete("OK")
    }

    Get("/").addHeader(validTokenWithPolicyMatchHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sPost using realmRole should return 200 OK when token is valid & has realmRole") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")
    import securityDirectives._

    val validTokenWithRealmRoleStr    = "validTokenWithRealmRoleStr"
    val validTokenWithRealmRole       = mock[AccessToken]
    val validTokenWithRealmRoleHeader = Authorization(OAuth2BearerToken(validTokenWithRealmRoleStr))
    when(validTokenWithRealmRole.hasRealmRole("admin"))
      .thenReturn(true)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithRealmRoleStr`) ⇒ Future.successful(Some(validTokenWithRealmRole))
      case _                                      ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = sPost(RealmRolePolicy("admin")) { _ ⇒
      complete("OK")
    }

    Post("/").addHeader(validTokenWithRealmRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sPut using permission should return 200 OK when token is valid & has permission") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")
    import securityDirectives._

    val validTokenWithPermissionStr    = "validTokenWithPermissionStr"
    val validTokenWithPermissionHeader = Authorization(OAuth2BearerToken(validTokenWithPermissionStr))

    val validTokenWithPermission = mock[AccessToken]

    when(validTokenWithPermission.hasPermission("read", "Default Resource"))
      .thenReturn(true)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithPermissionStr`) ⇒ Future.successful(Some(validTokenWithPermission))
      case _                                       ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = sPut(PermissionPolicy("read")) { _ ⇒
      complete("OK")
    }

    Put("/").addHeader(validTokenWithPermissionHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sDelete using resourceRole should return 200 OK when token is valid & has resourceRole") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")
    import securityDirectives._

    val validTokenWithResourceRoleStr    = "validTokenWithResourceRoleStr"
    val validTokenWithResourceRole       = mock[AccessToken]
    val validTokenWithResourceRoleHeader = Authorization(OAuth2BearerToken(validTokenWithResourceRoleStr))
    when(validTokenWithResourceRole.hasResourceRole("admin", "test"))
      .thenReturn(true)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithResourceRoleStr`) ⇒ Future.successful(Some(validTokenWithResourceRole))
      case _                                         ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = sDelete(ResourceRolePolicy("admin")) { _ ⇒
      complete("OK")
    }

    Delete("/").addHeader(validTokenWithResourceRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sHead using resourceRole should return 200 OK when token is valid & has resourceRole") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")
    import securityDirectives._

    val validTokenWithResourceRoleStr    = "validTokenWithResourceRoleStr"
    val validTokenWithResourceRole       = mock[AccessToken]
    val validTokenWithResourceRoleHeader = Authorization(OAuth2BearerToken(validTokenWithResourceRoleStr))
    when(validTokenWithResourceRole.hasResourceRole("admin", "test"))
      .thenReturn(true)

    val authenticator: AsyncAuthenticator[AccessToken] = {
      case Provided(`validTokenWithResourceRoleStr`) ⇒ Future.successful(Some(validTokenWithResourceRole))
      case _                                         ⇒ Future.successful(None)
    }

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = sHead(ResourceRolePolicy("admin")) { _ ⇒
      complete("OK")
    }

    Head("/").addHeader(validTokenWithResourceRoleHeader) ~> route ~> check {
      status shouldBe StatusCodes.OK
    }
  }

  test("sPatch using customPolicy should return AuthenticationFailedRejection when token is not present") {
    val authentication: Authentication = mock[Authentication]
    val securityDirectives             = new SecurityDirectives(authentication, "TMT", "test")
    import securityDirectives._

    val authenticator: AsyncAuthenticator[AccessToken] = _ ⇒ Future.successful(None)

    when(authentication.authenticator).thenReturn(authenticator)

    val route: Route = sPatch(CustomPolicy(_ => false)) { _ ⇒
      complete("OK")
    }

    Patch("/") ~> route ~> check {
      rejection shouldBe a[AuthenticationFailedRejection]
    }
  }
}
