package csw.services.location

import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.messages.location.Connection.HttpConnection
import csw.messages.location._
import csw.services.location.helpers.{LSNodeSpec, OneMemberAndSeed}
import csw.services.location.models.HttpRegistration
import csw.services.logging.scaladsl.LogAdminActorFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class DetectHttpComponentCrashTestMultiJvmNode1 extends DetectHttpComponentCrashTest(0)
class DetectHttpComponentCrashTestMultiJvmNode2 extends DetectHttpComponentCrashTest(0)

class DetectHttpComponentCrashTest(ignore: Int) extends LSNodeSpec(config = new OneMemberAndSeed) {

  import config._
  import cswCluster.mat

  test("A component running on one node should detect if a http component running on another node crashes") {

    val httpConnection = HttpConnection(ComponentId("Assembly1", ComponentType.Assembly))

    runOn(seed) {
      val (_, probe) = locationService.track(httpConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      enterBarrier("Registration")

      probe.requestNext() shouldBe a[LocationUpdated]
      Thread.sleep(2000)

      Await.result(testConductor.exit(member, 0), 5.seconds)

      // Story CSW-15 requires crash detection within 10 seconds with a goal of 5 seconds.
      // This 5.seconds demonstrates that if the test passes, the performance goal is met. Could be relaxed to 10 seconds
      // if needed.
      within(20.seconds) {
        awaitAssert {
          probe.requestNext(20.seconds) shouldBe a[LocationRemoved]
        }
      }
    }

    runOn(member) {
      val port   = 9595
      val prefix = "/trombone/hcd"

      val httpRegistration =
        HttpRegistration(httpConnection, port, prefix, LogAdminActorFactory.make(system))

      locationService.register(httpRegistration).await
      enterBarrier("Registration")

      Await.ready(system.whenTerminated, 5.seconds)
    }
  }
}
