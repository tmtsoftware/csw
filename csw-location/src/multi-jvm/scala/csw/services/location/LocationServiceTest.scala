package csw.services.location

import akka.actor.{Actor, Props}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.common.TestFutureExtension.RichFuture
import csw.services.location.helpers.LSNodeConfig.OneMemberAndSeed
import csw.services.location.helpers.LSNodeSpec
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.{CswCluster, LocationServiceFactory}

class LocationServiceTestMultiJvmNode1 extends LocationServiceTest(0)
class LocationServiceTestMultiJvmNode2 extends LocationServiceTest(0)

class LocationServiceTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) {

  import config._

  private val cswCluster = CswCluster.withSystem(system)
  private val locationService = LocationServiceFactory.withCluster(cswCluster)
  import cswCluster.mat

  test("ensure that the cluster is up") {
    awaitAssert {
      DistributedData(system).replicator ! GetReplicaCount
      expectMsg(ReplicaCount(roles.size))
    }
    enterBarrier("after-1")
  }

  test("ensure that a component registered by one node is resolved and listed on all the nodes") {
    val tcpPort = 446
    val tcpConnection = TcpConnection(ComponentId("reddis", ComponentType.Service))
    val tcpRegistration = TcpRegistration(tcpConnection, tcpPort)

    val httpPort = 81
    val httpPath = "/test/hcd"
    val httpConnection = HttpConnection(ComponentId("tromboneHcd", ComponentType.HCD))
    val httpRegistration = HttpRegistration(httpConnection, httpPort, httpPath)

    runOn(seed) {
      locationService.register(tcpRegistration).await
      enterBarrier("Registration")

      Thread.sleep(1000)
      val resolvedHttpLocation = locationService.resolve(httpConnection).await.get
      resolvedHttpLocation.connection shouldBe httpConnection

      val locations = locationService.list.await
      val connections = locations.map(_.connection)
      connections.toSet shouldBe Set(tcpConnection, httpConnection)
    }

    runOn(member1) {
      locationService.register(httpRegistration).await
      enterBarrier("Registration")

      Thread.sleep(1000)
      val resolvedTcpLocation = locationService.resolve(tcpConnection).await.get
      resolvedTcpLocation.connection shouldBe tcpConnection

      val locations = locationService.list.await
      val connections = locations.map(_.connection)
      connections.toSet shouldBe Set(tcpConnection, httpConnection)
    }

    enterBarrier("after-2")
  }

//  This test is doing the same thing what TrackLocationTest is doing
//  but the plan is to run this test on two amazon instance's with Jenkins configuration (multi-node-test).
  test("ensure that a component registered on one node is tracked on all the nodes") {
    val componentId = ComponentId("tromboneHcd", ComponentType.HCD)
    val akkaConnection = AkkaConnection(componentId)

    runOn(seed) {
      val actorRef = cswCluster.actorSystem.actorOf(
        Props(new Actor {
          override def receive: Receive = Actor.emptyBehavior
        }),
        "trombone-hcd"
      )
      locationService.register(AkkaRegistration(akkaConnection, actorRef)).await
      enterBarrier("Registration")

      locationService.unregister(akkaConnection).await
      enterBarrier("Unregister")
    }

    runOn(member1) {
      val (switch, probe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

      probe.request(1)
      probe.requestNext() shouldBe a[LocationUpdated]
      enterBarrier("Registration")

      probe.request(1)
      probe.requestNext() shouldBe a[LocationRemoved]
      enterBarrier("Unregister")

      switch.shutdown()
      probe.request(1)
      probe.expectComplete()
    }
    enterBarrier("after-3")
  }
}
