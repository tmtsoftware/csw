package csw.services.location.models

import csw.services.location.models.Connection.AkkaConnection
import org.scalatest.{FunSuite, Matchers}

class ConnectionTest extends FunSuite with Matchers {

  test("should able to form a connection name with componentId name, componentId type and connection type separated by '-'") {
    val expectedAkkaConnectionName = "tromboneHcd-hcd-akka"
    val akkaConnection = AkkaConnection(ComponentId("tromboneHcd", ComponentType.HCD))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

}
