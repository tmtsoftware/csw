package csw.location.models.scaladsl

import csw.location.models.ComponentType
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ComponentTypeTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  // DEOPSCSW-14: Codec for data modelCSW-80
  //CSW-80: Prefix should be in lowercase
  test(
    "ComponentType should be any one of this types : 'container', 'hcd', 'assembly', 'sequence', 'sequence_component', 'service' and 'machine'"
  ) {

    val expectedComponentTypeValues = Set("container", "hcd", "assembly", "service", "sequencer", "sequence_component", "machine")
    val actualComponentTypeValues: Set[String] =
      ComponentType.values.map(componentType => componentType.entryName).toSet

    actualComponentTypeValues shouldEqual expectedComponentTypeValues
  }
}
