package csw.services.location.models

import csw.services.location.models.ComponentType._
import org.scalatest.{FunSuite, Matchers}

import scala.util.{Failure, Success}

class ComponentTypeTest
    extends FunSuite
    with Matchers {

  test("successfully parses component type string representations") {
    ComponentType.parse("container").shouldEqual(Success(Container))
    ComponentType.parse("assembly").shouldEqual(Success(Assembly))
    ComponentType.parse("hcd").shouldEqual(Success(HCD))
    ComponentType.parse("service").shouldEqual(Success(Service))
  }

  test("should fail to parse invalid component type string") {
    val parsedComponent = ComponentType.parse("invalid component")
    parsedComponent.shouldEqual(Failure(UnknownComponentTypeException("invalid component")))
  }

}
