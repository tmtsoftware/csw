package csw.auth.token
import csw.auth.TokenVerificationFailure.InvalidToken
import csw.auth.token.claims.{Access, Authorization, Permission}
import csw.auth.{Keycloak, KeycloakTokenVerifier, TokenVerifier}
import org.keycloak.exceptions.TokenSignatureInvalidException
import org.keycloak.representations.{AccessToken ⇒ KeycloakAccessToken}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{EitherValues, FunSuite, Matchers}

import scala.util.Failure

class AccessTokenTest extends FunSuite with MockitoSugar with Matchers with EitherValues {
  test("should able to check permissions for access token") {
    val permission: Set[Permission] = Set(Permission("test-resource-id", "test-resource", Option(Set("test-scope"))))
    val accessToken                 = AccessToken(authorization = Some(Authorization(Some(permission))))

    accessToken.hasPermission("test-scope", "test-resource") shouldEqual true
  }

  test("should able to check permissions if it does not exist in access token") {
    val accessToken = AccessToken(authorization = Some(Authorization(None)))

    accessToken.hasPermission("test-scope", "test-resource") shouldEqual false
  }

  test("should able to check permissions if authorization does not exist in access token") {
    val accessToken = AccessToken()

    accessToken.hasPermission("test-scope", "test-resource") shouldEqual false
  }

  test("should able to check permissions if scope is invalid") {
    val accessToken = AccessToken()

    accessToken.hasPermission("invalid-scope", "test-resource") shouldEqual false
  }

  test("should able to check permissions if resource is invalid") {
    val accessToken = AccessToken()

    accessToken.hasPermission("test-scope", "invalid-resource") shouldEqual false
  }

  test("should able to check realm role for access token") {
    val accessToken = AccessToken(realm_access = Some(Access(Some(Set("test-realm-role")))))

    accessToken.hasRealmRole("test-realm-role") shouldEqual true
  }

  test("should fail check for realm role") {
    val accessToken = AccessToken(realm_access = Some(Access(Some(Set("test-realm-role")))))

    accessToken.hasRealmRole("invalid-realm-role") shouldEqual false
  }

  test("should able to check resource role for access token") {
    val resourceAccess: Option[Map[String, Access]] = Some(Map("test-resource" -> Access(Some(Set("test-resource-role")))))
    val accessToken                                 = AccessToken(resource_access = resourceAccess)

    accessToken.hasResourceRole("test-resource-role") shouldEqual true
  }

  test("should fail check for resource role if role is present for other resource") {
    val resourceAccess: Option[Map[String, Access]] = Some(Map("other-resource" -> Access(Some(Set("test-resource-role")))))
    val accessToken                                 = AccessToken(resource_access = resourceAccess)

    accessToken.hasResourceRole("test-resource-role") shouldEqual false
  }

  test("should fail check for resource role") {
    val resourceAccess: Option[Map[String, Access]] = Some(Map("test-resource" -> Access(Some(Set("test-resource-role")))))
    val accessToken                                 = AccessToken(resource_access = resourceAccess)

    accessToken.hasResourceRole("invalid-resource-role") shouldEqual false
  }

  test("should throw exception while verifyAndDecode token") {
    val keycloakTokenVerifier  = mock[KeycloakTokenVerifier]
    val token                  = "test-token"
    val deployment             = Keycloak.deployment
    val keycloakAccessToken    = new KeycloakAccessToken()
    val tmtTokenVerifier       = new TokenVerifier(keycloakTokenVerifier)
    val validationExceptionMsg = "invalid token"
    val validationException    = new TokenSignatureInvalidException(keycloakAccessToken, validationExceptionMsg)

    when(keycloakTokenVerifier.verifyToken(token, deployment)).thenReturn(Failure(validationException))

    tmtTokenVerifier.verifyAndDecode(token).left.value shouldBe InvalidToken(validationExceptionMsg)
  }
}
