package csw.location.models.scaladsl

import csw.location.models.ComponentType
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

class ComponentTypeTest extends FunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  // DEOPSCSW-14: Codec for data model
  test("ComponentType should be any one of this types : 'container', 'hcd', 'assembly', 'sequence', 'sequencecomponent' and 'service'") {

    val expectedComponentTypeValues = Set("container", "hcd", "assembly", "service", "sequencer", "sequencecomponent")
    val actualComponentTypeValues: Set[String] =
      ComponentType.values.map(componentType => componentType.entryName).toSet

    actualComponentTypeValues shouldEqual expectedComponentTypeValues
  }
}
