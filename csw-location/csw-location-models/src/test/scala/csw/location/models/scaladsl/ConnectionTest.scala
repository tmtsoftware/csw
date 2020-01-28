package csw.location.models.scaladsl

import csw.location.models
import csw.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.location.models.{ComponentId, ComponentType, Connection}
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

// CSW-86: Subsystem should be case-insensitive
class ConnectionTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {
  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for akka connection for trombone HCD") {
    val expectedAkkaConnectionName = "NFIRAOS.tromboneHcd-hcd-akka"
    val akkaConnection             = AkkaConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneHcd"), ComponentType.HCD))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for tcp connection for redis") {
    val expectedTcpConnectionName = "CSW.redis-service-tcp"
    val tcpConnection             = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "redis"), ComponentType.Service))
    tcpConnection.name shouldBe expectedTcpConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for http connection for config service") {
    val expectedHttpConnectionName = "CSW.config-service-http"
    val httpConnection             = HttpConnection(models.ComponentId(Prefix(Subsystem.CSW, "config"), ComponentType.Service))
    httpConnection.name shouldBe expectedHttpConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for akka connection for trombone container") {
    val expectedAkkaConnectionName = "Container.tromboneContainer-container-akka"
    val akkaConnection =
      AkkaConnection(models.ComponentId(Prefix(Subsystem.Container, "tromboneContainer"), ComponentType.Container))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a string representation for akka connection for trombone assembly") {
    val expectedAkkaConnectionName = "NFIRAOS.tromboneAssembly-assembly-akka"
    val akkaConnection             = AkkaConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneAssembly"), ComponentType.Assembly))
    akkaConnection.name shouldBe expectedAkkaConnectionName
  }

  // DEOPSCSW-14: Codec for data model
  test("should able to form a connection for components from a valid string representation") {
    Connection.from("nfiraos.tromboneAssembly-assembly-akka") shouldBe
    AkkaConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneAssembly"), ComponentType.Assembly))

    Connection.from("nfiraos.tromboneHcd-hcd-akka") shouldBe
    AkkaConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneHcd"), ComponentType.HCD))

    Connection.from("csw.redis-service-tcp") shouldBe
    TcpConnection(models.ComponentId(Prefix(Subsystem.CSW, "redis"), ComponentType.Service))

    Connection.from("csw.configService-service-http") shouldBe
    HttpConnection(models.ComponentId(Prefix(Subsystem.CSW, "configService"), ComponentType.Service))
  }

  // DEOPSCSW-14: Codec for data model
  test("should not be able to form a connection for components from an invalid string representation") {
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
