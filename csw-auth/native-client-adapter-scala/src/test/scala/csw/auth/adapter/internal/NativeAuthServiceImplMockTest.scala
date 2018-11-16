package csw.auth.adapter.internal

import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled
import org.keycloak.representations.AccessTokenResponse
import org.mockito.Mockito.{verify, when}
import org.scalatest.FunSuite
import org.scalatest.mockito.MockitoSugar

class NativeAuthServiceImplMockTest extends FunSuite with MockitoSugar {

  class AuthMocks {
    val keycloakInstalled: KeycloakInstalled     = mock[KeycloakInstalled]
    val kd: KeycloakDeployment                   = mock[KeycloakDeployment]
    val store: FileAuthStore                     = mock[FileAuthStore]
    val accessTokenResponse: AccessTokenResponse = mock[AccessTokenResponse]

    val accessToken  = "access_token"
    val idToken      = "id_token"
    val refreshToken = "refresh_token"

    when(keycloakInstalled.getTokenResponse).thenReturn(accessTokenResponse)
    when(keycloakInstalled.getTokenResponse).thenReturn(accessTokenResponse)
    when(keycloakInstalled.getDeployment).thenReturn(kd)
    when(accessTokenResponse.getToken).thenReturn(accessToken)
    when(accessTokenResponse.getIdToken).thenReturn(idToken)
    when(accessTokenResponse.getRefreshToken).thenReturn(refreshToken)

    when(store.getAccessTokenString).thenReturn(Some(accessToken))
    when(store.getIdTokenString).thenReturn(Some(idToken))
    when(store.getRefreshTokenString).thenReturn(Some(refreshToken))

    val authService = new NativeAuthServiceImpl(keycloakInstalled, Some(store))
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

    authService.getAccessToken()
    verify(store).getRefreshTokenString
    verify(keycloakInstalled).refreshToken(refreshToken)
    verify(keycloakInstalled).getTokenResponse
    verify(store).saveTokens(idToken, accessToken, refreshToken)
  }

  test("logout") {
    val mocks = new AuthMocks
    import mocks._

    authService.logout()
    verify(keycloakInstalled).logout()
    verify(store).clearStorage()
  }

}
