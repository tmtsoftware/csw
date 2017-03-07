package csw.services.location.models

import csw.services.location.models.Connection.InvalidConnectionStringException
import org.scalatest.{FunSuite, Matchers}

class ConnectionTest extends FunSuite with Matchers {

  test("should successfully parse a valid Connection string representation.") {
    Connection.parse("lgsTromboneHCD-hcd-akka").get shouldBe Connection(
      ComponentId("lgsTromboneHCD", ComponentType.HCD),
      ConnectionType.AkkaType
    )
  }

  test("should fail to parse connection string with more than 3 parts") {
    intercept[InvalidConnectionStringException] {
      Connection.parse("a-b-c-d").get
    }
  }

  test("should fail to parse connection string with invalid component type") {
    intercept[InvalidConnectionStringException] {
      Connection.parse("HCD1-invalidComp-akka").get
    }
  }

  test("should fail to parse connection string with invalid connection type") {
    intercept[InvalidConnectionStringException] {
      Connection.parse("HCD1-hcd-invalidConnection").get
    }
  }

}
