package csw.services.location.models

import org.scalatest.{FunSuite, Matchers}

class ComponentTypeTest extends FunSuite with Matchers {

  test("ComponentType should be any one of this types : 'container', 'hcd', 'assembly' and 'service'") {

    val expectedComponentTypeValues = Set("container", "hcd", "assembly", "service")
    val actualComponentTypeValues: Set[String] =
      ComponentType.values.map(componentType => componentType.entryName).toSet

    actualComponentTypeValues shouldEqual expectedComponentTypeValues
  }
}
