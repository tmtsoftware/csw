package csw.services.location

import akka.actor.{Actor, ActorSystem, Props}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.commons.ClusterSettings
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.services.location.models.Connection.AkkaConnection
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class DetectComponentRestartTestMultiJvmNode1 extends DetectComponentRestartTest(0)
class DetectComponentRestartTestMultiJvmNode2 extends DetectComponentRestartTest(0)
class DetectComponentRestartTestMultiJvmNode3 extends DetectComponentRestartTest(0)

class DetectComponentRestartTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._

  import cswCluster.mat

  test("ensure that the cluster is up") {
    enterBarrier("nodes-joined")
    awaitAssert {
      DistributedData(system).replicator ! GetReplicaCount
      expectMsg(ReplicaCount(roles.size))
    }
    enterBarrier("after-1")
  }

  test("should detect re-registering of new location for a connection that has crashed/gone away") {
    val akkaConnection = AkkaConnection(ComponentId("TromboneHcd", ComponentType.HCD))

    runOn(seed) {
      val (switch, probe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      enterBarrier("Registration")

      probe.requestNext() shouldBe a[LocationUpdated]

      Thread.sleep(2000)
      Await.result(testConductor.shutdown(member2), 10.seconds)

      probe.requestNext(5.seconds) shouldBe a[LocationRemoved]

      enterBarrier("member-restarted")

      probe.requestNext(5.seconds) shouldBe a[LocationUpdated]
    }

    runOn(member1) {
      val (switch, probe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      enterBarrier("Registration")

      probe.requestNext() shouldBe a[LocationUpdated]

      Thread.sleep(2000)
      probe.requestNext(5.seconds) shouldBe a[LocationRemoved]

      enterBarrier("member-restarted")

      probe.requestNext(5.seconds) shouldBe a[LocationUpdated]
    }

    runOn(member2) {
      val actorRef = cswCluster.actorSystem
        .actorOf(
          Props(new Actor {
            override def receive: Receive = Actor.emptyBehavior
          }),
          "trombone-hcd-1"
        )
      locationService.register(AkkaRegistration(akkaConnection, actorRef)).await
      enterBarrier("Registration")
      Await.ready(system.whenTerminated, 10.seconds)

      val actorSystem: ActorSystem = startNewSystem()

      val freshActorRef = actorSystem
        .actorOf(
          Props(new Actor {
            override def receive: Receive = Actor.emptyBehavior
          }),
          "trombone-hcd-1"
        )

      val freshLocationService = LocationServiceFactory.withSettings(ClusterSettings().joinLocal(3552))
      Thread.sleep(1000)
      freshLocationService.register(AkkaRegistration(akkaConnection, freshActorRef))

      enterBarrier("member-restarted")
    }
    enterBarrier("after-2")
  }

}
