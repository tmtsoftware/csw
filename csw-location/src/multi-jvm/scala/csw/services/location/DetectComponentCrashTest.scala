package csw.services.location

import akka.actor.{Actor, Props}
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.services.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.services.location.models._
import csw.services.location.scaladsl.ActorSystemFactory
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

class DetectComponentCrashTestMultiJvmNode1 extends DetectComponentCrashTest(0)
class DetectComponentCrashTestMultiJvmNode2 extends DetectComponentCrashTest(0)
class DetectComponentCrashTestMultiJvmNode3 extends DetectComponentCrashTest(0)

/**
 * This test is running as a part of jenkins master-slave setup forming three nodes cluster. (seed: running on jenkins master, member1: running on jenkins slave, member2: running on jenkins slave)
 * This test exercises below steps :
 * 1. Registering akka connection on member1 node
 * 2. seed(master) and member2 is tracking a akka connection which is registered on slave (member1)
 * 3. Exiting member1 using testConductor.exit(member1, 1) (tell the remote node to shut itself down using System.exit with the given
 * exitValue 1. The node will also be removed from cluster)
 * 4. Once remote member1 is exited, we are asserting that master (seed) and member2 should receive LocationRemoved event within 5 seconds
 * => probe.requestNext(5.seconds) shouldBe a[LocationRemoved]
 *
**/
class DetectComponentCrashTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._
  import cswCluster.mat

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  test("component running on one node should detect if other component running on another node crashes") {

    val akkaConnection = AkkaConnection(ComponentId("Container1", ComponentType.Container))

    runOn(seed) {
      val (switch, probe) = locationService.track(akkaConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      enterBarrier("Registration")

      probe.requestNext() shouldBe a[LocationUpdated]
      Thread.sleep(2000)

      Await.result(testConductor.exit(member1, 0), 5.seconds)
      enterBarrier("after-crash")

      // Story CSW-15 requires crash detection within 10 seconds with a goal of 5 seconds.
      // This 5.seconds demonstrates that if the test passes, the performance goal is met. Could be relaxed to 10 seconds
      // if needed.
      within(5.seconds) {
        awaitAssert {
          probe.requestNext(5.seconds) shouldBe a[LocationRemoved]
        }
      }

      Thread.sleep(2000)

      locationService.list.await.size shouldBe 1
    }

    runOn(member1) {
      val actorRef = ActorSystemFactory
        .remote()
        .actorOf(
          Props(new Actor {
            override def receive: Receive = Actor.emptyBehavior
          }),
          "trombone-hcd-1"
        )
      locationService.register(AkkaRegistration(akkaConnection, actorRef)).await
      enterBarrier("Registration")

      Await.ready(system.whenTerminated, 5.seconds)
    }

    runOn(member2) {
      val port   = 9595
      val prefix = "/trombone/hcd"

      val httpConnection   = HttpConnection(ComponentId("Assembly1", ComponentType.Assembly))
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
