package csw.services.location

import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.messages.location.Connection.HttpConnection
import csw.messages.location._
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.services.location.models.HttpRegistration
import csw.services.logging.scaladsl.{LogAdminActorFactory, LoggingSystemFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

class DetectHttpComponentCrashTestMultiJvmNode1 extends DetectHttpComponentCrashTest(0)
class DetectHttpComponentCrashTestMultiJvmNode2 extends DetectHttpComponentCrashTest(0)

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
class DetectHttpComponentCrashTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) {

  import config._
  import cswCluster.mat
  LoggingSystemFactory.start("", "", "", system)
  test("http component running on one node should detect if other component running on another node crashes") {

    val httpConnection = HttpConnection(ComponentId("Assembly1", ComponentType.Assembly))

    runOn(seed) {
      val (_, probe) = locationService.track(httpConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      enterBarrier("Registration")

      probe.requestNext() shouldBe a[LocationUpdated]
      Thread.sleep(2000)

      Await.result(testConductor.exit(member, 0), 5.seconds)
      enterBarrier("after-crash")

      // Story CSW-15 requires crash detection within 10 seconds with a goal of 5 seconds.
      // This 5.seconds demonstrates that if the test passes, the performance goal is met. Could be relaxed to 10 seconds
      // if needed.
      within(5.seconds) {
        awaitAssert {
          probe.requestNext(5.seconds) shouldBe a[LocationRemoved]
        }
      }
    }

    runOn(member) {
      val port   = 9595
      val prefix = "/trombone/hcd"

      val httpRegistration = HttpRegistration(httpConnection, port, prefix, LogAdminActorFactory.make(system))

      locationService.register(httpRegistration).await
      enterBarrier("Registration")

      Await.ready(system.whenTerminated, 5.seconds)
    }
  }
}
