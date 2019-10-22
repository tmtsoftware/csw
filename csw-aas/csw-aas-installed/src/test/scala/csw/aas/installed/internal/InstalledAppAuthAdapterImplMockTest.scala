package csw.aas.installed.internal

import csw.aas.core.TokenVerificationFailure.TokenExpired
import csw.aas.core.TokenVerifier
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.AccessToken
import csw.aas.installed.scaladsl.FileAuthStore
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.adapters.installed.KeycloakInstalled
import org.keycloak.representations.AccessTokenResponse
import org.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

//DEOPSCSW-575: Client Library for AAS to be accessed by CSW cli apps
class InstalledAppAuthAdapterImplMockTest extends FunSuite with MockitoSugar with Matchers {

  class AuthMocks {
    val keycloakInstalled: KeycloakInstalled     = mock[KeycloakInstalled]
    val kd: KeycloakDeployment                   = mock[KeycloakDeployment]
    val store: FileAuthStore                     = mock[FileAuthStore]
    val accessTokenResponse: AccessTokenResponse = mock[AccessTokenResponse]
    val tokenVerifier: TokenVerifier             = mock[TokenVerifier]
    val accessToken: AccessToken                 = mock[AccessToken]
    val refreshedAccessToken: AccessToken        = mock[AccessToken]
    val authConfig: AuthConfig                   = mock[AuthConfig]

    val accessTokenStr          = "access_token"
    val idTokenStr              = "id_token"
    val refreshTokenStr         = "refresh_token"
    val refreshedAccessTokenStr = "refreshed_access_token"

    when(authConfig.getDeployment).thenReturn(kd)

    // mock keycloakInstalled calls
    when(keycloakInstalled.getTokenResponse).thenReturn(accessTokenResponse)
    when(keycloakInstalled.getDeployment).thenReturn(kd)

    doNothing.when(keycloakInstalled).login()

    // mock keycloak's access token response calls
    when(accessTokenResponse.getToken).thenReturn(accessTokenStr)
    when(accessTokenResponse.getIdToken).thenReturn(idTokenStr)
    when(accessTokenResponse.getRefreshToken).thenReturn(refreshTokenStr)

    // mock auth store calls
    when(store.getAccessTokenString).thenReturn(Some(accessTokenStr))
    when(store.getIdTokenString).thenReturn(Some(idTokenStr))
    when(store.getRefreshTokenString).thenReturn(Some(refreshTokenStr))

    // mock token verifier's calls
    when(tokenVerifier.verifyAndDecode(accessTokenStr)).thenReturn(Future.successful(Right(accessToken)))
    when(tokenVerifier.verifyAndDecode(refreshedAccessTokenStr)).thenReturn(Future.successful(Right(refreshedAccessToken)))

    val authService = new InstalledAppAuthAdapterImpl(authConfig, keycloakInstalled, tokenVerifier, Some(store))
//    val disabledAuthService = new InstalledAppAuthAdapterImpl(authConfig, keycloakInstalled, tokenVerifier, Some(store))
  }

  test("login") {
    val mocks = new AuthMocks
    import mocks._

    authService.login()
    verify(keycloakInstalled).login()
    verify(keycloakInstalled).getTokenResponse
    verify(store).saveTokens(idTokenStr, accessTokenStr, refreshTokenStr)
  }

  test("loginDesktop") {
    val mocks = new AuthMocks
    import mocks._

    authService.loginDesktop()
    verify(keycloakInstalled).loginDesktop()
    verify(keycloakInstalled).getTokenResponse
    verify(store).saveTokens(idTokenStr, accessTokenStr, refreshTokenStr)
  }

  test("loginManual") {
    val mocks = new AuthMocks
    import mocks._

    authService.loginManual()
    verify(keycloakInstalled).loginManual()
    verify(keycloakInstalled).getTokenResponse
    verify(store).saveTokens(idTokenStr, accessTokenStr, refreshTokenStr)
  }

  test("logout") {
    val mocks = new AuthMocks
    import mocks._

    authService.logout()
    verify(keycloakInstalled).logout()
    verify(store).clearStorage()
  }

  test("getAccessToken") {
    val mocks = new AuthMocks
    import mocks._

    val maxTokenValidity = 5
    val tokenExp         = (System.currentTimeMillis() / 1000) + maxTokenValidity
    val token            = AccessToken(exp = Some(tokenExp))

    when(tokenVerifier.verifyAndDecode(accessTokenStr)).thenReturn(Future.successful(Right(token)))

    authService.getAccessToken() shouldBe Some(token)

    verify(store).getAccessTokenString
    verify(tokenVerifier).verifyAndDecode(accessTokenStr)
  }

  test("getAccessToken - disabled auth") {
    val mocks = new AuthMocks
    import mocks._

    when(authConfig.disabled).thenReturn(true)

    authService.getAccessToken() shouldBe Some(AccessToken())
  }

  test("getAccessToken - expired token") {
    val mocks = new AuthMocks
    import mocks._

    when(tokenVerifier.verifyAndDecode(accessTokenStr)).thenReturn(Future.successful(Left(TokenExpired)))
    when(store.getAccessTokenString).thenReturn(Some(accessTokenStr)).andThen(Some(refreshedAccessTokenStr))

    authService.getAccessToken() shouldBe Some(refreshedAccessToken)

    verify(store, times(2)).getAccessTokenString
    verify(tokenVerifier).verifyAndDecode(accessTokenStr)
    verify(keycloakInstalled).refreshToken(refreshTokenStr)
    verify(tokenVerifier).verifyAndDecode(refreshedAccessTokenStr)
  }

  test("getAccessToken - expired token with min validity") {
    val mocks = new AuthMocks
    import mocks._

    val maxTokenValidity  = 5
    val minValidityNeeded = 10
    val tokenExp          = (System.currentTimeMillis() / 1000) + maxTokenValidity

    when(tokenVerifier.verifyAndDecode(accessTokenStr)).thenReturn(Future.successful(Right(AccessToken(exp = Some(tokenExp)))))
    when(store.getAccessTokenString).thenReturn(Some(accessTokenStr)).andThen(Some(refreshedAccessTokenStr))

    authService.getAccessToken(minValidityNeeded.seconds) shouldBe Some(refreshedAccessToken)

    verify(tokenVerifier).verifyAndDecode(accessTokenStr)
    verify(store, times(2)).getAccessTokenString
    verify(keycloakInstalled).refreshToken(refreshTokenStr)
    verify(tokenVerifier).verifyAndDecode(refreshedAccessTokenStr)
  }
}
