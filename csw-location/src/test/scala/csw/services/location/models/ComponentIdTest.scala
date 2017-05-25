package csw.services.location.models

import csw.services.logging.utils.CswTestSuite

class ComponentIdTest extends CswTestSuite {

  override protected def afterAllTests(): Unit = ()

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
