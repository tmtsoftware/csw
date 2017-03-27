package csw.services.location

import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
import csw.services.location.helpers.{LSMultiNodeConfig, LSMultiNodeSpec}
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.TcpConnection
import csw.services.location.models.{ComponentId, ComponentType, TcpLocation, TcpRegistration}
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}

class SimpleRegistrationTestMultiJvmNode1 extends SimpleRegistrationTest(0)
class SimpleRegistrationTestMultiJvmNode2 extends SimpleRegistrationTest(0)

class SimpleRegistrationTest(ignore: Int) extends LSMultiNodeSpec(new LSMultiNodeConfig) {

  import config._

  private val actorRuntime = new ActorRuntime(system)
  private val locationService = LocationServiceFactory.make(actorRuntime)

  test("ensure that the cluster is up") {
    awaitAssert {
      DistributedData(system).replicator ! GetReplicaCount
      expectMsg(ReplicaCount(roles.size))
    }
    enterBarrier("after-1")
  }

  test("ensure that a component registered by one node is listed on all the nodes") {
    val tcpRegistration = TcpRegistration(TcpConnection(ComponentId("abc", ComponentType.Service)),  10)

    runOn(node1) {
      locationService.register(tcpRegistration)
    }

    runOn(node2) {
      Thread.sleep(1000)
      locationService.list.await shouldBe List(tcpRegistration.location(Networks.hostname()))
    }

    enterBarrier("after-2")
  }
}
