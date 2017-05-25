package csw.services.location.models

import csw.services.logging.utils.CswTestSuite

class ComponentTypeTest extends CswTestSuite {

  override protected def afterAllTests(): Unit = ()

  test("ComponentType should be any one of this types : 'container', 'hcd', 'assembly' and 'service'") {

    val expectedComponentTypeValues = Set("container", "hcd", "assembly", "service")
    val actualComponentTypeValues: Set[String] =
      ComponentType.values.map(componentType => componentType.entryName).toSet

    actualComponentTypeValues shouldEqual expectedComponentTypeValues
  }
}
