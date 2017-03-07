package csw.services.location.models

import org.scalatest.{FunSuite, Matchers}

class ConnectionTest extends FunSuite with Matchers {

  test("should successfully parse Akka Connection string representation.") {
    Connection.parse("lgsTromboneHCD-hcd-akka").get shouldBe Connection(
      ComponentId("lgsTromboneHCD", ComponentType.HCD),
      ConnectionType.AkkaType
    )
  }

  test("should fail to parse invalid connection string") {
    intercept[RuntimeException] {
      Connection.parse("abcd").get
    }
  }

}
