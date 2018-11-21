package csw.auth.token
import csw.auth.token.claims.{Access, Authorization, Permission}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

class AccessTokenTest extends FunSuite with MockitoSugar with Matchers {
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
    val accessToken = AccessToken(realm_access = Some(Access(Some(Set("test-role")))))

    accessToken.hasRole("test-role") shouldEqual true
  }

  test("should able to check resource role for access token") {
    val resourceAccess: Option[Map[String, Access]] = Some(Map("test-resource" -> Access(Some(Set("test-resource-role")))))
    val accessToken                                 = AccessToken(resource_access = resourceAccess)

    accessToken.hasRole("test-resource-role") shouldEqual true
  }

  test("should able to check role if role is present for other resource") {
    val resourceAccess: Option[Map[String, Access]] = Some(Map("other-resource" -> Access(Some(Set("test-resource-role")))))
    val accessToken                                 = AccessToken(resource_access = resourceAccess)

    accessToken.hasRole("test-resource-role") shouldEqual false
  }

  test("should able to check role if role is not present for that resource") {
    val resourceAccess: Option[Map[String, Access]] = Some(Map("test-resource" -> Access(Some(Set("test-resource-role")))))
    val accessToken                                 = AccessToken(resource_access = resourceAccess)

    accessToken.hasRole("invalid-resource-role") shouldEqual false
  }
}
