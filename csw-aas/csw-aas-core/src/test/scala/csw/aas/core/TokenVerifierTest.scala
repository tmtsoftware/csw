package csw.aas.core

import csw.aas.core.TokenVerificationFailure.{InvalidToken, TokenExpired}
import csw.aas.core.deployment.AuthConfig
import csw.aas.core.token.AccessToken
import csw.aas.core.token.claims.{Access, Audience, Authorization}
import csw.aas.core.utils.Conversions.RichEitherTFuture
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.exceptions.{TokenNotActiveException, TokenSignatureInvalidException}
import org.keycloak.representations.{AccessToken ⇒ KeycloakAccessToken}
import org.mockito.MockitoSugar
import org.scalatest.{EitherValues, FunSuite, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class TokenVerifierTest extends FunSuite with MockitoSugar with Matchers with EitherValues {
  private val keycloakTokenVerifier: KeycloakTokenVerifier = mock[KeycloakTokenVerifier]
  private val authConfig: AuthConfig                       = mock[AuthConfig]
  private val deployment: KeycloakDeployment               = authConfig.getDeployment

  when(authConfig.getDeployment).thenReturn(deployment)

  private val keycloakAccessToken: KeycloakAccessToken = new KeycloakAccessToken()
  private val tmtTokenVerifier: TokenVerifier          = new TokenVerifier(keycloakTokenVerifier, authConfig)

  test("should throw exception while verifyAndDecode token") {
    val token                  = "test-token"
    val validationExceptionMsg = "invalid token"
    val validationException    = new TokenSignatureInvalidException(keycloakAccessToken, validationExceptionMsg)

    when(keycloakTokenVerifier.verifyToken(token, deployment)).thenReturn(Future.failed(validationException))

    tmtTokenVerifier.verifyAndDecode(token).block().left.value shouldBe InvalidToken(validationExceptionMsg)
  }

  test("should throw TokenExpired exception while verifying token") {
    val token                    = "test-token"
    val tokenExpiredExceptionMsg = "Token expired"
    val tokenExpiredException    = new TokenNotActiveException(keycloakAccessToken, tokenExpiredExceptionMsg)

    when(keycloakTokenVerifier.verifyToken(token, deployment)).thenReturn(Future.failed(tokenExpiredException))

    tmtTokenVerifier.verifyAndDecode(token).block().left.value shouldBe TokenExpired
  }

  test("should verifyAndDecode token") {
    val token =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJneWlMbndmWWR3SmNENUdnUnJXMUFCNmNzeVBLZWFPb2lMZHp4dnFBeUZjIn0.eyJqdGkiOiJlN2IzNzAwYi0wNjgxLTQ4MzQtOWE0Zi0yMjU5YmUzOWRiZTUiLCJleHAiOjE1NDI4MjI2MTcsIm5iZiI6MCwiaWF0IjoxNTQyODIyNTU3LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoibmV3LXRlc3QiLCJzdWIiOiI3ZGQ0NmU2ZS01YTk4LTRlZTYtOTFiZS02OWJmYjYwN2ZlYjQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJuZXctdGVzdCIsIm5vbmNlIjoiYWNjODJmYWMtYzRkNS00NDhmLTg0NDQtNmJmZGQ0MzQ4YWMzIiwiYXV0aF90aW1lIjoxNTQyODIyNTU2LCJzZXNzaW9uX3N0YXRlIjoiMmU4OTdlNDctZTlhMi00YTQ4LWI4MjAtNWJlMTE4YmY5YjVmIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIqIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoiZW1tYSIsImdpdmVuX25hbWUiOiIiLCJmYW1pbHlfbmFtZSI6IiJ9.DFT0YnDoOmm9fp0xrR7ObzB9th_WVzDi3cNPEWURgHjtJUqWhA_gyl3fs6LKDt00rL82jVLhA8T7L8BeclgubcCDD8iytb_Nr_xtBdCTZ47YSjXfHsUD6x4s8ltHVMflaPEqMoL2JrBio552Z-6denhKzcdvUXePBDcZrdkxnLIIwA5pbc6ZM4LWJFpDBqdzH0r2a-DBMCEqdRK-o_aqx43c8H0KPXJtftR74YF4WV5X8uASsEIYL6P31inhK1r0fv0ifdTNpkoHaAG_qST4ppD14pvWCKylpT5jSM5hfc-H78m3PPwb93nv3JVC5kMO-yBmStEe9fyTSPCOd97eoQ"
    val expectedToken = AccessToken(
      sub = Option("7dd46e6e-5a98-4ee6-91be-69bfb607feb4"),
      iat = Option(1542822557),
      exp = Option(1542822617),
      iss = Option("http://localhost:8080/auth/realms/master"),
      aud = Audience("new-test"),
      jti = Option("e7b3700b-0681-4834-9a4f-2259be39dbe5"),
      given_name = Option(""),
      family_name = Option(""),
      name = None,
      preferred_username = Option("emma"),
      email = None,
      scope = Option("openid email profile"),
      realm_access = Access(Set("offline_access", "uma_authorization")),
      resource_access = Map("account" -> Access(Set("manage-account", "manage-account-links", "view-profile"))),
      authorization = Authorization.empty
    )

    when(keycloakTokenVerifier.verifyToken(token, deployment)).thenReturn(Future.successful(keycloakAccessToken))
    tmtTokenVerifier.verifyAndDecode(token).block().right.value shouldBe expectedToken
  }

  test("should throw exception while decoding token") {
    val invalidJsonToken     = "ey.wer.erwe.werw"
    val expectedExceptionMsg = "Expected token [ey.wer.erwe.werw] to be composed of 2 or 3 parts separated by dots."

    when(keycloakTokenVerifier.verifyToken(invalidJsonToken, deployment)).thenReturn(Future.successful(keycloakAccessToken))
    tmtTokenVerifier.verifyAndDecode(invalidJsonToken).block().left.value shouldBe InvalidToken(expectedExceptionMsg)
  }
}
