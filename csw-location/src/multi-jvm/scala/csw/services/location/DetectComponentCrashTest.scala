package csw.services.location

import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import csw.messages.location.Connection.{HttpConnection, TcpConnection}
import csw.messages.location._
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.services.location.models.{HttpRegistration, TcpRegistration}
import csw.services.logging.scaladsl.LogAdminActorFactory

import scala.concurrent.Await
import scala.concurrent.duration._

class DetectComponentCrashTestMultiJvmNode1 extends DetectComponentCrashTest(0)
class DetectComponentCrashTestMultiJvmNode2 extends DetectComponentCrashTest(0)
class DetectComponentCrashTestMultiJvmNode3 extends DetectComponentCrashTest(0)

class DetectComponentCrashTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._
  import cswCluster.mat

  test("A component running on one node should detect if a http/tcp component running on another node crashes") {

    val httpConnection = HttpConnection(ComponentId("Assembly1", ComponentType.Assembly))
    val tcpConnection1 = TcpConnection(ComponentId("Assembly2", ComponentType.Assembly))
    val tcpConnection2 = TcpConnection(ComponentId("Assembly3", ComponentType.Assembly))

    runOn(seed) {
      val (_, httpProbe) = locationService.track(httpConnection).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      val (_, tcpProbe1) = locationService.track(tcpConnection1).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      val (_, tcpProbe2) = locationService.track(tcpConnection2).toMat(TestSink.probe[TrackingEvent])(Keep.both).run()
      enterBarrier("Registration")

      httpProbe.requestNext() shouldBe a[LocationUpdated]
      tcpProbe1.requestNext() shouldBe a[LocationUpdated]
      tcpProbe2.requestNext() shouldBe a[LocationUpdated]
      Thread.sleep(2000)

      Await.result(testConductor.exit(member1, 0), 5.seconds)
      within(20.seconds) {
        awaitAssert {
          httpProbe.requestNext(20.seconds) shouldBe a[LocationRemoved]
        }
      }

      enterBarrier("after-crash")
      Await.result(testConductor.exit(member2, 0), 5.seconds)

      within(20.seconds) {
        awaitAssert {
          tcpProbe1.requestNext(20.seconds) shouldBe a[LocationRemoved]
          tcpProbe2.requestNext(20.seconds) shouldBe a[LocationRemoved]
        }
      }
    }

    runOn(member1) {
      val port   = 9595
      val prefix = "/trombone/hcd"

      val httpRegistration =
        HttpRegistration(httpConnection, port, prefix, LogAdminActorFactory.make(system))

      locationService.register(httpRegistration).await
      enterBarrier("Registration")

      Await.ready(system.whenTerminated, 5.seconds)
    }

    runOn(member2) {
      val port             = 9595
      val tcpRegistration1 = TcpRegistration(tcpConnection1, port, LogAdminActorFactory.make(system))
      val tcpRegistration2 = TcpRegistration(tcpConnection2, port, LogAdminActorFactory.make(system))

      locationService.register(tcpRegistration1).await
      locationService.register(tcpRegistration2).await
      enterBarrier("Registration")

      enterBarrier("after-crash")
      Await.ready(system.whenTerminated, 5.seconds)
    }
  }
}
