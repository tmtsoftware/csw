package csw.services.location.models

import csw.services.location.models.ComponentType.HCD
import org.scalatest.{FunSuite, Matchers}

class ComponentIdTest extends FunSuite with Matchers {

  test("Should successfully parse Component ID") {
    val componentId: ComponentId = ComponentId.parse("lgsTromboneHCD-HCD").get
    componentId.name shouldEqual "lgsTromboneHCD"
    componentId.componentType should be (HCD)
  }

}
