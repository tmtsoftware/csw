package csw.aas.http

import csw.commons.RandomUtils
import csw.prefix.models.Subsystem
import msocket.security.models.{Access, AccessToken}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.util.Random

class RolesTest extends AnyFunSuite with Matchers {

  test("apply using roles should convert roles to lower case") {
    Roles(Set("IRIS_ENG", "ESW_USER")).roles shouldBe Set("esw_user", "iris_eng")
  }

  test("apply using token should convert roles to lower case") {
    val accessToken = AccessToken(realm_access = Access(Set("IRIS_ENG", "ESW_USER")))
    Roles(accessToken).roles shouldBe Set("esw_user", "iris_eng")
  }

  test("containsUserRole should give true if user role for given subsystem is present") {
    val subsystem = RandomUtils.randomFrom(Subsystem.values)
    val roles     = Roles(Set(s"$subsystem-user"))

    roles.containsUserRole(subsystem) shouldBe true
  }

  test("containsUserRole should give false if user role for given subsystem is not present") {
    val subsystems      = Subsystem.values.toList
    val roles           = Roles(Set(s"${subsystems.head}-user"))
    val randomSubsystem = RandomUtils.randomFrom(subsystems.tail) // random subsystem other than the one present in roles

    roles.containsUserRole(randomSubsystem) shouldBe false
  }

  test("containsEngRole should give true if eng role for given subsystem is present") {
    val subsystem = RandomUtils.randomFrom(Subsystem.values)
    val roles     = Roles(Set(s"$subsystem-eng"))

    roles.containsEngRole(subsystem) shouldBe true
  }

  test("containsEngRole should give false if eng role for given subsystem is not present") {
    val subsystems      = Subsystem.values.toList
    val roles           = Roles(Set(s"${subsystems.head}-eng"))
    val randomSubsystem = RandomUtils.randomFrom(subsystems.tail) // random subsystem other than the one present in roles

    roles.containsEngRole(randomSubsystem) shouldBe false
  }

  test("containsAnyRole should give true if roles contain any role of given subsystem") {
    val subsystem1 = RandomUtils.randomFrom(Subsystem.values)
    val subsystem2 = RandomUtils.randomFrom(Subsystem.values)
    val subsystem3 = RandomUtils.randomFrom(Subsystem.values)

    val roles = Roles(Set(s"$subsystem1-eng", s"$subsystem2-admin", s"$subsystem3-user"))

    roles.containsAnyRole(subsystem1) shouldBe true
    roles.containsAnyRole(subsystem2) shouldBe true
    roles.containsAnyRole(subsystem3) shouldBe true
  }

  test("containsAnyRole should give false if roles does not contain any role of given subsystem") {
    val subsystem1 = RandomUtils.randomFrom(Subsystem.values)
    val subsystem2 = RandomUtils.randomFrom(Subsystem.values)
    val subsystem3 = RandomUtils.randomFrom(Subsystem.values)

    val roles = Roles(Set.empty[String])

    roles.containsAnyRole(subsystem1) shouldBe false
    roles.containsAnyRole(subsystem2) shouldBe false
    roles.containsAnyRole(subsystem3) shouldBe false
  }

  test("exist should check if roles have a common entry") {
    val subSystems = Random.shuffle(Subsystem.values)

    val subsystem1 = subSystems(0)
    val subsystem2 = subSystems(1)
    val subsystem3 = subSystems(2)

    val commonRole = s"$subsystem2-admin".toLowerCase
    val roles1     = Roles(Set(s"$subsystem1-user", commonRole))
    val roles2     = Roles(Set(s"$subsystem3-user", commonRole))
    val roles3     = Roles(Set(s"$subsystem1-user"))

    roles1.intersect(roles2) shouldBe Set(commonRole)
    roles2.intersect(roles3) shouldBe Set.empty
  }
}
