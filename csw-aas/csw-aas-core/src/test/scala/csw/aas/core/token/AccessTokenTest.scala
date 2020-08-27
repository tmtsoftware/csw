package csw.aas.core.token

import csw.aas.core.token.claims.Access
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

//DEOPSCSW-579: Prevent unauthorized access based on akka http route rules
class AccessTokenTest extends AnyFunSuite with Matchers {

  test("should able to check realm role for access token | DEOPSCSW-579") {
    val accessToken = AccessToken(realm_access = Access(Set("test-realm-role")))

    accessToken.hasRealmRole("test-realm-role") shouldEqual true
  }

  test("should fail check for realm role | DEOPSCSW-579") {
    val accessToken = AccessToken(realm_access = Access(Set("test-realm-role")))

    accessToken.hasRealmRole("invalid-realm-role") shouldEqual false
  }
}
