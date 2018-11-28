package csw.auth.core.token
import csw.auth.core.token.claims.{Access, Authorization, Permission}
import org.scalatest.{FunSuite, Matchers}

class AccessTokenTest extends FunSuite with Matchers {

  test("should able to check permissions for access token") {
    val permission: Set[Permission] = Set(Permission("test-resource-id", "test-resource", Option(Set("test-scope"))))
    val accessToken                 = AccessToken(authorization = Some(Authorization(permission)))

    accessToken.hasPermission("test-scope", "test-resource") shouldEqual true
  }

  test("should able to check permissions if it does not exist in access token") {
    val accessToken = AccessToken(authorization = Some(Authorization()))

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
    val accessToken = AccessToken(realm_access = Some(Access(Set("test-realm-role"))))

    accessToken.hasRealmRole("test-realm-role") shouldEqual true
  }

  test("should fail check for realm role") {
    val accessToken = AccessToken(realm_access = Some(Access(Set("test-realm-role"))))

    accessToken.hasRealmRole("invalid-realm-role") shouldEqual false
  }

  test("should able to check resource role for access token") {
    val resourceAccess: Map[String, Access] = Map("test-resource" -> Access(Set("test-resource-role")))
    val accessToken                         = AccessToken(resource_access = resourceAccess)

    accessToken.hasResourceRole("test-resource-role") shouldEqual true
  }

  test("should fail check for resource role if role is present for other resource") {
    val resourceAccess: Map[String, Access] = Map("other-resource" -> Access(Set("test-resource-role")))
    val accessToken                         = AccessToken(resource_access = resourceAccess)

    accessToken.hasResourceRole("test-resource-role") shouldEqual false
  }

  test("should fail check for resource role") {
    val resourceAccess: Map[String, Access] = Map("test-resource" -> Access(Set("test-resource-role")))
    val accessToken                         = AccessToken(resource_access = resourceAccess)

    accessToken.hasResourceRole("invalid-resource-role") shouldEqual false
  }
}
