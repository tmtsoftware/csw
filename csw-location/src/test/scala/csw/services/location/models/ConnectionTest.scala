package csw.services.location.models

import csw.services.location.models.ConnectionType.{AkkaType, HttpType, TcpType}
import org.scalatest.{FunSuite, Matchers}

class ConnectionTest extends FunSuite  with Matchers{

  test("should successfully parse Akka Connection string representation.") {
    val akkaConnection: Connection = Connection.parse("lgsTromboneHCD-HCD-akka").get
    akkaConnection.componentId shouldEqual ComponentId.parse("lgsTromboneHCD-HCD").get
    akkaConnection.name shouldEqual "lgsTromboneHCD"
    akkaConnection.connectionType should be (AkkaType)
  }

  test("should successfully parse Http Connection string representation.") {
    val httpConnection: Connection = Connection.parse("lgsTromboneHCD-HCD-http").get
    httpConnection.componentId shouldEqual ComponentId.parse("lgsTromboneHCD-HCD").get
    httpConnection.name shouldEqual "lgsTromboneHCD"
    httpConnection.connectionType should be (HttpType)
  }

  test("should successfully parse Tcp Connection string representation.") {
    val tcpConnection: Connection = Connection.parse("lgsTromboneHCD-HCD-tcp").get
    tcpConnection.componentId shouldEqual ComponentId.parse("lgsTromboneHCD-HCD").get
    tcpConnection.name shouldEqual "lgsTromboneHCD"
    tcpConnection.connectionType should be (TcpType)
  }

  test("should fail to parse invalid connection string") {
//    val ftpConnection: Connection = Connection.parse("lgsTromboneHCD-HCD-ftp").get
//    pending implementation
  }

}
