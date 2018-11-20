package csw.auth.adapters.nativeapp.internal

import csw.auth.token.AccessToken
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled
import org.keycloak.representations.AccessTokenResponse
import org.keycloak.test.FluentTestsHelper
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.duration.DurationDouble

class NativeAppAuthAdapterImplMockTest extends FunSuite with MockitoSugar with Matchers {

  class AuthMocks {
    val keycloakInstalled: KeycloakInstalled     = mock[KeycloakInstalled]
    val kd: KeycloakDeployment                   = mock[KeycloakDeployment]
    val store: FileAuthStore                     = mock[FileAuthStore]
    val accessTokenResponse: AccessTokenResponse = mock[AccessTokenResponse]
    val keycloakHelper: FluentTestsHelper        = new FluentTestsHelper()

    val accessToken  = "access_token"
    val idToken      = "id_token"
    val refreshToken = "refresh_token"

    // mock keycloakInstalled calls
    when(keycloakInstalled.getTokenResponse).thenReturn(accessTokenResponse)
    when(keycloakInstalled.getDeployment).thenReturn(kd)

    // mock keycloak's access token response calls
    when(accessTokenResponse.getToken).thenReturn(accessToken)
    when(accessTokenResponse.getIdToken).thenReturn(idToken)
    when(accessTokenResponse.getRefreshToken).thenReturn(refreshToken)

    // mock auth store calls
    when(store.getAccessTokenString).thenReturn(Some(accessToken))
    when(store.getIdTokenString).thenReturn(Some(idToken))
    when(store.getRefreshTokenString).thenReturn(Some(refreshToken))

    val authService = new NativeAppAuthAdapterImpl(keycloakInstalled, Some(store))
  }

  test("login") {
    val mocks = new AuthMocks
    import mocks._

    authService.login()
    verify(keycloakInstalled).login()
    verify(keycloakInstalled).getTokenResponse
    verify(store).saveTokens(idToken, accessToken, refreshToken)
  }

  test("loginDesktop") {
    val mocks = new AuthMocks
    import mocks._

    authService.loginDesktop()
    verify(keycloakInstalled).loginDesktop()
    verify(keycloakInstalled).getTokenResponse
    verify(store).saveTokens(idToken, accessToken, refreshToken)
  }

  test("loginManual") {
    val mocks = new AuthMocks
    import mocks._

    authService.loginManual()
    verify(keycloakInstalled).loginManual()
    verify(keycloakInstalled).getTokenResponse
    verify(store).saveTokens(idToken, accessToken, refreshToken)
  }

  test("getAccessToken") {
    val mocks = new AuthMocks
    import mocks._

    val token = new FluentTestsHelper().init().getToken

    when(store.getAccessTokenString).thenReturn(Some(token))

    authService.getAccessToken() shouldBe AccessToken.verifyAndDecode(token).toOption
    verify(store).getAccessTokenString
  }

  test("logout") {
    val mocks = new AuthMocks
    import mocks._

    authService.logout()
    verify(keycloakInstalled).logout()
    verify(store).clearStorage()
  }

  test("refreshing token") {
    val mocks = new AuthMocks
    import mocks._

    val token          = new FluentTestsHelper().init().getToken
    val refreshedToken = new FluentTestsHelper().init().getToken

    when(store.getAccessTokenString).thenReturn(Some(token)).thenReturn(Some(refreshedToken))

    authService.getAccessToken(200.seconds) shouldBe AccessToken.verifyAndDecode(refreshedToken).toOption

    verify(store, times(2)).getAccessTokenString
    verify(keycloakInstalled).refreshToken(refreshToken)
  }

}
