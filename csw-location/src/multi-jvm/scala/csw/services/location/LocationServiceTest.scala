package csw.services.location

import java.net.URI

import akka.actor.{Actor, Props}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
import akka.remote.testconductor.RoleName
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.helpers.{LSMultiNodeConfig, LSMultiNodeSpec}
import csw.services.location.internal.Networks
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorRuntime, LocationServiceFactory}
import org.scalatest.Matchers

class LocationServiceTestMultiJvmNode1 extends LocationServiceTest(0)
class LocationServiceTestMultiJvmNode2 extends LocationServiceTest(0)

class LocationServiceTest(ignore: Int)
  extends LSMultiNodeSpec(new LSMultiNodeConfig)
    with Matchers {

  import config._

  private val actorRuntime = new ActorRuntime(system)
  private val locationService = LocationServiceFactory.make(actorRuntime)
  import actorRuntime.{cluster, mat}

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

    runOn(node1) {
      locationService.register(tcpRegistration)
      Thread.sleep(1000)
      enterBarrier("Registration")

      val resolvedHttpLocation = locationService.resolve(httpConnection).await.get
      resolvedHttpLocation shouldBe HttpLocation(httpConnection, new URI(s"http://${new Networks().hostname()}:$httpPort/$httpPath"))

      locationService.list.await.toSet shouldBe Set(tcpRegistration.location(new Networks().hostname()), httpRegistration.location(new Networks().hostname()))
    }

    runOn(node2) {
      locationService.register(httpRegistration)
      enterBarrier("Registration")

      val resolvedTcpLocation = locationService.resolve(tcpConnection).await.get
      resolvedTcpLocation shouldBe TcpLocation(tcpConnection, new URI(s"tcp://${new Networks().hostname()}:$tcpPort"))

      locationService.list.await.toSet shouldBe Set(tcpRegistration.location(new Networks().hostname()), httpRegistration.location(new Networks().hostname))
    }

    enterBarrier("after-2")
  }

  test("ensure that a component registered on one node is tracked on all the nodes") {
    val componentId = ComponentId("tromboneHcd", ComponentType.HCD)
    val akkaConnection = AkkaConnection(componentId)

    runOn(node1) {
      val actorRef = actorRuntime.actorSystem.actorOf(
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

    runOn(node2) {
      val (switch, probe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()

      probe.request(1)
      probe.expectNextPF({
        case LocationUpdated(_) => ()
      })
      enterBarrier("Registration")
      
      probe.request(1)
      probe.expectNextPF({
        case LocationRemoved(_) => ()
      })
      enterBarrier("Unregister")

      switch.shutdown()
      probe.request(1)
      probe.expectComplete()
    }
    enterBarrier("after-3")
  }
}
