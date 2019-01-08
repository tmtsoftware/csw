package csw.aas.core.token

import csw.aas.core.token.claims.{Access, Audience, Authorization, Permission}
import org.keycloak.authorization.client.resource.AuthorizationResource
import org.keycloak.authorization.client.{AuthorizationDeniedException, AuthzClient}
import org.keycloak.representations.idm.authorization.AuthorizationResponse
import org.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-578: Programming Interface for accessing userinfo
class RPTTest extends FunSuite with MockitoSugar with Matchers with ScalaFutures {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(Span(1, Seconds), Span(1, Seconds))

  test("should create accessToken") {
    val authzClient           = mock[AuthzClient]
    val authorizationResource = mock[AuthorizationResource]
    val authorizationResponse = mock[AuthorizationResponse]
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
    val rpt = RPT(authzClient)

    when(authzClient.authorization(token)).thenReturn(authorizationResource)
    when(authorizationResource.authorize()).thenReturn(authorizationResponse)
    when(authorizationResponse.getToken).thenReturn(token)

    rpt.create(token).value.futureValue shouldEqual Right(expectedToken)
  }

  test("should create RPTn") {
    val authzClient           = mock[AuthzClient]
    val authorizationResource = mock[AuthorizationResource]
    val authorizationResponse = mock[AuthorizationResponse]
    val tokenStr =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJudHRFLUFaRGxhTThYdWt6QlhXWnJKYkFPNDRwM1pSSUFWWDVWQXpOQlMwIn0.eyJqdGkiOiIyMjBmNTMzYi00ODYyLTRjZTYtYmUzNi0zOWI1MzZjNjllOTkiLCJleHAiOjE1NDM0NzUwMTEsIm5iZiI6MCwiaWF0IjoxNTQzNDc0NDExLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvZXhhbXBsZSIsImF1ZCI6WyJleGFtcGxlLWFwcCIsImV4YW1wbGUtc2VydmVyIl0sInN1YiI6IjI0YzBhZTc4LWQyZDMtNDk1NS1iNzFmLWY2ZTZjZjdkZWE3OCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImV4YW1wbGUtYXBwIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiNDIxOGRkMTUtN2JiMS00Y2I0LWFmZWItOGYzYWQyMzQ1MGQ3IiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIiwiZXhhbXBsZS1hZG1pbi1yb2xlIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiZXhhbXBsZS1zZXJ2ZXIiOnsicm9sZXMiOlsicGVyc29uLXJvbGUiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwibmFtZSI6InRlc3QtdXNlciIsInByZWZlcnJlZF91c2VybmFtZSI6InRlc3QtdXNlciIsImdpdmVuX25hbWUiOiJ0ZXN0LXVzZXIifQ.IOtHNp4LRqEz8AFkH5g1EQAYNN1kbex35wo9JIEcSTQe-NKKNFdr_yr1LlarIbatCEUrSXi9vV3RmX0CY2F2bkIbUhzYJKubI0tw5Ym3xcc_LSEQFNJu00_H-kQ3AVrgg__mfuHlmF5vTAdu_eaSoLnuirPNre_LwZPqBDcNVaYQt1tXXt2E4lD8Thbdgl5c0HcdaN7XzJGuPHkE3GxhKVqrlvsRoTvVaMLmyW-qIAQm8Nuu_GfUbZpxUhITtyP5b_tWMCzRfiXknGIWHjVwfeASN8_geXZDH8S8UEa3Qmv13ByPM8mdnZrzNYNRQlnIScFYCCRbIsP3eyEIhZBp3g"

    val rptStr =
      "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJudHRFLUFaRGxhTThYdWt6QlhXWnJKYkFPNDRwM1pSSUFWWDVWQXpOQlMwIn0.eyJqdGkiOiJmNDkzZmMzOC03MzVhLTRjM2QtYTJlNy01OTI4ZThhOGUwYzEiLCJleHAiOjE1NDM0NzU1MDcsIm5iZiI6MCwiaWF0IjoxNTQzNDc0OTA3LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMvZXhhbXBsZSIsImF1ZCI6WyJleGFtcGxlLWFwcCIsImV4YW1wbGUtc2VydmVyIl0sInN1YiI6IjI0YzBhZTc4LWQyZDMtNDk1NS1iNzFmLWY2ZTZjZjdkZWE3OCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImV4YW1wbGUtYXBwIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiY2YwM2Y4MDUtMWEwMy00NWRlLWE5NmEtYTY4Nzk2MDJjODkzIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIiwiZXhhbXBsZS1hZG1pbi1yb2xlIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiZXhhbXBsZS1zZXJ2ZXIiOnsicm9sZXMiOlsicGVyc29uLXJvbGUiXX0sImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sImF1dGhvcml6YXRpb24iOnsicGVybWlzc2lvbnMiOlt7InNjb3BlcyI6WyJkZWxldGUiXSwicnNpZCI6ImYxZDQ1MTRkLTRiZmItNDhlMi05NDQ4LWY5MmE5NGZmY2E0ZCIsInJzbmFtZSI6InBlcnNvbiJ9LHsicnNpZCI6ImU3MGNjMDhiLTVlYWQtNDIyOC1hZmIzLWE3NTUwM2MxYmYzNyIsInJzbmFtZSI6IkRlZmF1bHQgUmVzb3VyY2UifV19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJ0ZXN0LXVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0LXVzZXIiLCJnaXZlbl9uYW1lIjoidGVzdC11c2VyIn0.hJHMycSHTGNoSREX3yhw0akxM59j4yCUN_gLfdkwWyvvpWoPoSL4zrkKk7xwO6C9NeyfjZKYTlBvVG7hV-XQnQhT9Oq7zTQrniv0vDSkna8ldkevgNnVFIJ-dMEL0VfFd0PrPSzHRWJHR_3yOE-nZ3UvJde2m0E-OpQAx_qjHZN66A6O2KkURP2hB4lg_N0AyizjVpLwAGDKksBGsn0SPeohFE8r6R7xbcu8__4OJTA_WKHppJeO4WtY0f9zGyZXi6wtNO0lHCerfLtBNJJFdrVkIpRGjNgsQHBe_gJJ0Cm-wxf_8A6CFP7qqKIpVlVEClScBJHLIWq-mieQxbT7hw"

    val expectedPRT = AccessToken(
      sub = Option("24c0ae78-d2d3-4955-b71f-f6e6cf7dea78"),
      iat = Option(1543474907),
      exp = Option(1543475507),
      iss = Option("http://localhost:8080/auth/realms/example"),
      aud = Audience(ArrayBuffer("example-app", "example-server")),
      jti = Option("f493fc38-735a-4c3d-a2e7-5928e8a8e0c1"),
      given_name = Option("test-user"),
      family_name = None,
      name = Option("test-user"),
      preferred_username = Option("test-user"),
      email = None,
      scope = Option("profile email"),
      realm_access = Access(Set("offline_access", "uma_authorization", "example-admin-role")),
      resource_access = Map("account"        → Access(Set("manage-account", "manage-account-links", "view-profile")),
                            "example-server" → Access(Set("person-role"))),
      authorization = Authorization(
        Set(
          Permission("f1d4514d-4bfb-48e2-9448-f92a94ffca4d", "person", Set("delete")),
          Permission("e70cc08b-5ead-4228-afb3-a75503c1bf37", "Default Resource", Set.empty)
        )
      )
    )
    val rpt = RPT(authzClient)

    when(authzClient.authorization(tokenStr)).thenReturn(authorizationResource)
    when(authorizationResource.authorize()).thenReturn(authorizationResponse)
    when(authorizationResponse.getToken).thenReturn(rptStr)

    rpt.create(tokenStr).value.futureValue shouldEqual Right(expectedPRT)
  }

  test("should fail for creating accessToken") {
    val authzClient           = mock[AuthzClient]
    val authorizationResource = mock[AuthorizationResource]
    val token                 = "asd.adsasd.asd.qwe"
    val rpt                   = RPT(authzClient)

    when(authzClient.authorization(token)).thenReturn(authorizationResource)
    when(authorizationResource.authorize())
      .thenThrow(new AuthorizationDeniedException("token is invalid", new RuntimeException))

    a[AuthorizationDeniedException] shouldBe thrownBy(Await.result(rpt.create(token).value, 5.seconds))
  }
}
