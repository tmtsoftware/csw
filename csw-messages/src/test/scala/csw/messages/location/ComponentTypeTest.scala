package csw.messages.location

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ComponentTypeTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  test("ComponentType should be any one of this types : 'container', 'hcd', 'assembly' and 'service'") {

    val expectedComponentTypeValues = Set("container", "hcd", "assembly", "service", "sequencer")
    val actualComponentTypeValues: Set[String] =
      ComponentType.values.map(componentType => componentType.entryName).toSet

    actualComponentTypeValues shouldEqual expectedComponentTypeValues
  }
}
