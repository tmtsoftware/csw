package csw.location

import csw.location.api.models
import csw.location.api.models.Connection.{HttpConnection, TcpConnection}
import csw.location.api.models.{ComponentId, ComponentType, HttpRegistration, TcpRegistration}
import csw.location.server.commons.TestFutureExtension.RichFuture
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.BeforeAndAfterEach

import scala.collection.immutable.Set
import scala.concurrent.duration._

class LocationServiceTestMultiJvmNode1 extends LocationServiceTest(0, "cluster")

class LocationServiceTestMultiJvmNode2 extends LocationServiceTest(0, "cluster")

class LocationServiceTest(ignore: Int, mode: String)
    extends helpers.LSNodeSpec(config = new helpers.OneMemberAndSeed, mode)
    with BeforeAndAfterEach {

  import config._

  // DEOPSCSW-16: Register a component
  test(s"${this.suiteName}:${myself.name} ensure that a component registered by one node is resolved and listed on all the nodes | DEOPSCSW-16, DEOPSCSW-429") {
    val tcpPort         = 446
    val tcpConnection   = TcpConnection(ComponentId(Prefix(Subsystem.CSW, "redis"), ComponentType.Service))
    val tcpRegistration = TcpRegistration(tcpConnection, tcpPort)

    val httpPort         = 81
    val httpPath         = "/test/hcd"
    val httpConnection   = HttpConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "tromboneHcd"), ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, httpPort, httpPath)

    runOn(seed) {
      locationService.register(tcpRegistration).await
      enterBarrier("Registration")

      val resolvedHttpLocation = locationService.resolve(httpConnection, 5.seconds).await.get
      resolvedHttpLocation.connection shouldBe httpConnection

      val locations   = locationService.list.await
      val connections = locations.map(_.connection)
      connections.toSet shouldBe Set(tcpConnection, httpConnection)
    }

    runOn(member) {
      locationService.register(httpRegistration).await
      enterBarrier("Registration")

      val resolvedTcpLocation = locationService.resolve(tcpConnection, 5.seconds).await.get
      resolvedTcpLocation.connection shouldBe tcpConnection

      val locations   = locationService.list.await
      val connections = locations.map(_.connection)
      connections.toSet shouldBe Set(tcpConnection, httpConnection)
    }

    enterBarrier("after-2")
  }

}
