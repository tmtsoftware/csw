package csw.services.location.scaladsl.models

import java.net.URI

import csw.services.location.scaladsl.models.Connection.AkkaConnection
import org.scalatest.FunSuite

class ResolvedAkkaLocationTest extends FunSuite {
  test("resolves actor reference") {
    val componentId = ComponentId("hcd1", ComponentType.HCD)
    val connection = AkkaConnection(componentId)
    ResolvedAkkaLocation(connection, new URI("akka://hcd1"), "prefix")
  }
}
