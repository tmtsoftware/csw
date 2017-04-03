package csw.services.location

import akka.actor.{Actor, Props}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class DetectComponentCrashTestMultiJvmNode1 extends DetectComponentCrashTest(0)
class DetectComponentCrashTestMultiJvmNode2 extends DetectComponentCrashTest(0)
class DetectComponentCrashTestMultiJvmNode3 extends DetectComponentCrashTest(0)

class DetectComponentCrashTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._

  private val locationService = LocationServiceFactory.withCluster(cswCluster)
  import cswCluster.mat

  test("ensure that the cluster is up") {
    enterBarrier("nodes-joined")
    awaitAssert {
      DistributedData(system).replicator ! GetReplicaCount
      expectMsg(ReplicaCount(roles.size))
    }
    enterBarrier("after-1")
  }

  test("component running on one node should detect if other component running on another node crashes"){

    val akkaConnection = AkkaConnection(ComponentId("Container1", ComponentType.Container))

    runOn(seed) {
      val (switch, probe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      enterBarrier("Registration")

      probe.requestNext() shouldBe a[LocationUpdated]
      Thread.sleep(2000)

      Await.result(testConductor.shutdown(member1, abort = true), 30.seconds)
      enterBarrier("after-crash")

      within(5.seconds) {
        awaitAssert {
          probe.requestNext(5.seconds) shouldBe a[LocationRemoved]
        }
      }

      Thread.sleep(2000)

      locationService.list.await.size shouldBe 1
    }

    runOn(member1) {
      val actorRef = cswCluster.actorSystem.actorOf(
        Props(new Actor {
          override def receive: Receive = Actor.emptyBehavior
        }),
        "trombone-hcd-1"
      )
      locationService.register(AkkaRegistration(akkaConnection, actorRef)).await
      enterBarrier("Registration")

      Await.ready(system.whenTerminated, 30.seconds)
    }

    runOn(member2) {
      val port = 9595
      val prefix = "/trombone/hcd"

      val httpConnection = HttpConnection(ComponentId("Assembly1", ComponentType.Assembly))
      val httpRegistration = HttpRegistration(httpConnection, port, prefix)

      locationService.register(httpRegistration).await

      enterBarrier("Registration")
      val (switch, probe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      Thread.sleep(2000)
      enterBarrier("after-crash")

      within(5.seconds) {
        awaitAssert {
          probe.requestNext(5.seconds) shouldBe a[LocationRemoved]
        }
      }

      Thread.sleep(2000)

      locationService.list.await.size shouldBe 1

    }
  }
}
