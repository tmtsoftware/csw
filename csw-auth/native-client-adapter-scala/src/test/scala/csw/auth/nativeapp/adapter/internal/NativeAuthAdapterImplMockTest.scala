package csw.auth.nativeapp.adapter.internal

import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled
import org.keycloak.representations.AccessTokenResponse
import org.mockito.Mockito.{verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class NativeAuthAdapterImplMockTest extends FunSuite with MockitoSugar with Matchers {

  class AuthMocks {
    val keycloakInstalled: KeycloakInstalled     = mock[KeycloakInstalled]
    val kd: KeycloakDeployment                   = mock[KeycloakDeployment]
    val store: FileAuthStore                     = mock[FileAuthStore]
    val accessTokenResponse: AccessTokenResponse = mock[AccessTokenResponse]

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

    val authService = new NativeAuthAdapterImpl(keycloakInstalled, Some(store))
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

    authService.getAccessToken() shouldBe None
    verify(store).getAccessTokenString
  }

  test("logout") {
    val mocks = new AuthMocks
    import mocks._

    authService.logout()
    verify(keycloakInstalled).logout()
    verify(store).clearStorage()
  }

}
