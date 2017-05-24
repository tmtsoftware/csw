package csw.services.location.models

import csw.services.location.CswTestSuite
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}

class ConnectionTest extends CswTestSuite {

  override protected def afterAllTests(): Unit = ()

  test("should able to form a string representation for akka connection for trombone HCD") {
    val expectedAkkaConnectionName = "tromboneHcd-hcd-akka"
    val akkaConnection             = AkkaConnection(ComponentId("tromboneHcd", ComponentType.HCD))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  test("should able to form a string representation for tcp connection for redis") {
    val expectedTcpConnectionName = "redis-service-tcp"
    val tcpConnection             = TcpConnection(ComponentId("redis", ComponentType.Service))
    tcpConnection.name shouldBe expectedTcpConnectionName
  }

  test("should able to form a string representation for http connection for config service") {
    val expectedHttpConnectionName = "config-service-http"
    val httpConnection             = HttpConnection(ComponentId("config", ComponentType.Service))
    httpConnection.name shouldBe expectedHttpConnectionName
  }

  test("should able to form a string representation for akka connection for trombone container") {
    val expectedAkkaConnectionName = "tromboneContainer-container-akka"
    val akkaConnection             = AkkaConnection(ComponentId("tromboneContainer", ComponentType.Container))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  test("should able to form a string representation for akka connection for trombone assembly") {
    val expectedAkkaConnectionName = "tromboneAssembly-assembly-akka"
    val akkaConnection             = AkkaConnection(ComponentId("tromboneAssembly", ComponentType.Assembly))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  test("should able to form a connection for trombone assembly from a valid string representation") {
    Connection.from("tromboneAssembly-assembly-akka") shouldBe
    AkkaConnection(ComponentId("tromboneAssembly", ComponentType.Assembly))

    Connection.from("tromboneHcd-hcd-akka") shouldBe
    AkkaConnection(ComponentId("tromboneHcd", ComponentType.HCD))

    Connection.from("redis-service-tcp") shouldBe
    TcpConnection(ComponentId("redis", ComponentType.Service))

    Connection.from("configService-service-http") shouldBe
    HttpConnection(ComponentId("configService", ComponentType.Service))
  }
}
