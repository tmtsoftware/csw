package csw.location.api.models.scaladsl

import csw.location.api.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.api.models.{ComponentId, ComponentType, Connection}
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

// CSW-86: Subsystem should be case-insensitive
class ConnectionTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for akka connection for trombone HCD | DEOPSCSW-14") {
    val expectedAkkaConnectionName = "NFIRAOS.tromboneHcd-HCD-akka"
    val akkaConnection             = AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneHcd"), ComponentType.HCD))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for tcp connection for redis | DEOPSCSW-14") {
    val expectedTcpConnectionName = "CSW.redis-Service-tcp"
    val tcpConnection             = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "redis"), ComponentType.Service))
    tcpConnection.name shouldBe expectedTcpConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for http connection for config service | DEOPSCSW-14") {
    val expectedHttpConnectionName = "CSW.config-Service-http"
    val httpConnection             = HttpConnection(ComponentId(Prefix(Subsystem.CSW, "config"), ComponentType.Service))
    httpConnection.name shouldBe expectedHttpConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for akka connection for trombone container | DEOPSCSW-14") {
    val expectedAkkaConnectionName = "Container.tromboneContainer-Container-akka"
    val akkaConnection =
      AkkaConnection(ComponentId(Prefix(Subsystem.Container, "tromboneContainer"), ComponentType.Container))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for akka connection for trombone assembly | DEOPSCSW-14") {
    val expectedAkkaConnectionName = "NFIRAOS.tromboneAssembly-Assembly-akka"
    val akkaConnection = AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneAssembly"), ComponentType.Assembly))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a connection for components from a valid string representation | DEOPSCSW-14") {
    Connection.from("nfiraos.tromboneAssembly-Assembly-akka") shouldBe
    AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneAssembly"), ComponentType.Assembly))

    Connection.from("nfiraos.tromboneHcd-HCD-akka") shouldBe
    AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneHcd"), ComponentType.HCD))

    Connection.from("csw.redis-Service-tcp") shouldBe
    TcpConnection(ComponentId(Prefix(Subsystem.CSW, "redis"), ComponentType.Service))

    Connection.from("csw.configService-Service-http") shouldBe
    HttpConnection(ComponentId(Prefix(Subsystem.CSW, "configService"), ComponentType.Service))
  }

  // DEOPSCSW-14: Codec for data model
  test("should not be able to form a connection for components from an invalid string representation | DEOPSCSW-14") {
    val connection = "nfiraos.tromboneAssembly_assembly_akka"
    val exception = intercept[IllegalArgumentException] {
      Connection.from(connection)
    }

    exception.getMessage shouldBe s"Unable to parse '$connection' to make Connection object"

    val connection2 = "nfiraos.trombone-hcd"
    val exception2 = intercept[IllegalArgumentException] {
      Connection.from(connection2)
    }

    exception2.getMessage shouldBe s"Unable to parse '$connection2' to make Connection object"
  }
}
