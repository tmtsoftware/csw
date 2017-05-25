package csw.services.location.models

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ComponentIdTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  test("should not contain leading or trailing spaces in component's name") {

    val illegalArgumentException = intercept[IllegalArgumentException] {
      ComponentId(" redis ", ComponentType.Service)
    }

    illegalArgumentException.getMessage shouldBe "requirement failed: component name has leading and trailing whitespaces"
  }

  test("should not contain '-' in component's name") {
    val illegalArgumentException = intercept[IllegalArgumentException] {
      ComponentId("redis-service", ComponentType.Service)
    }

    illegalArgumentException.getMessage shouldBe "requirement failed: component name has '-'"
  }
}
