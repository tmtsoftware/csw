package csw.services.location

import akka.testkit.TestProbe
import akka.typed.Behavior
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.messages.location.Connection.AkkaConnection
import csw.messages.location.{ComponentId, ComponentType, LocationRemoved, LocationUpdated}
import csw.services.location.commons.CswCluster
import csw.services.location.helpers.{LSNodeSpec, TwoMembersAndSeed}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationServiceFactory
import org.jboss.netty.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

import scala.concurrent.Await
import scala.concurrent.duration._

class DetectComponentRestartTestMultiJvmNode1 extends DetectComponentRestartTest(0)
class DetectComponentRestartTestMultiJvmNode2 extends DetectComponentRestartTest(0)
class DetectComponentRestartTestMultiJvmNode3 extends DetectComponentRestartTest(0)

class DetectComponentRestartTest(ignore: Int) extends LSNodeSpec(config = new TwoMembersAndSeed) {

  import config._

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory)

  test("should detect re-registering of new location for a connection that has crashed/gone away") {

    val akkaConnection = AkkaConnection(ComponentId("TromboneHcd", ComponentType.HCD))

    runOn(member1) {
      locationService
        .register(
          AkkaRegistration(akkaConnection,
                           Some("nfiraos.ncc.trombone"),
                           system.spawnAnonymous(Behavior.empty),
                           system.spawnAnonymous(Behavior.empty))
        )
        .await
      enterBarrier("location-registered")
      enterBarrier("location-updated")

      Await.ready(system.whenTerminated, 10.seconds)

      val newSystem = startNewSystem()

      val freshLocationService = LocationServiceFactory.withCluster(CswCluster.withSystem(newSystem))
      Thread.sleep(2000)

      freshLocationService
        .register(
          AkkaRegistration(akkaConnection,
                           Some("nfiraos.ncc.trombone"),
                           newSystem.spawnAnonymous(Behavior.empty),
                           newSystem.spawnAnonymous(Behavior.empty))
        )
        .await
      enterBarrier("member-re-registered")
    }

    runOn(seed, member2) {
      enterBarrier("location-registered")
      val testProbe = TestProbe()
      locationService.subscribe(akkaConnection, testProbe.testActor ! _)

      testProbe.expectMsgType[LocationUpdated]
      enterBarrier("location-updated")

      runOn(seed) {
        Await.result(testConductor.shutdown(member1), 10.seconds)
      }

      testProbe.expectMsgType[LocationRemoved](5.seconds)
      Thread.sleep(2000)
      enterBarrier("member-re-registered")
      testProbe.expectMsgType[LocationUpdated](5.seconds)
    }

    enterBarrier("after-2")
  }

}
