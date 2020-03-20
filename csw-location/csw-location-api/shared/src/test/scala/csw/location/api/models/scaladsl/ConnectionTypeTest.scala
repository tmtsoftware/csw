package csw.location.api.models.scaladsl

import csw.location.api.models.ConnectionType
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

class ConnectionTypeTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  // DEOPSCSW-14: Codec for data model
  test("ConnectionType should be any one of this types : 'http', 'tcp' and 'akka' | DEOPSCSW-14") {

    val expectedConnectionTypeValues = Set("http", "tcp", "akka")

    val actualConnectionTypeValues: Set[String] =
      ConnectionType.values.map(connectionType => connectionType.entryName).toSet

    actualConnectionTypeValues shouldEqual expectedConnectionTypeValues
  }

}
