package csw.services.location.models

import csw.services.logging.utils.CswTestSuite

class ConnectionTypeTest extends CswTestSuite {

  override protected def afterAllTests(): Unit = ()

  test("ConnectionType should be any one of this types : 'http', 'tcp' and 'akka'") {

    val expectedConnectionTypeValues = Set("http", "tcp", "akka")

    val actualConnectionTypeValues: Set[String] =
      ConnectionType.values.map(connectionType => connectionType.entryName).toSet

    actualConnectionTypeValues shouldEqual expectedConnectionTypeValues
  }

}
