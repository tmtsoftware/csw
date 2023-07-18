/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.SpawnProtocol
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.stream.scaladsl.{Keep, Sink}
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection}
import csw.location.api.models._
import csw.location.api.{PekkoRegistrationFactory, models}
import csw.location.client.ActorSystemFactory
import csw.logging.client.commons.PekkoTypedExtension.UserActorFactory
import csw.prefix.models.{Prefix, Subsystem}
import org.scalatest.OptionValues
import org.scalatest.concurrent.Eventually

import scala.concurrent.Await
import scala.concurrent.duration._

class DetectPekkoComponentCrashTestMultiJvmNode1 extends DetectPekkoComponentCrashTest(0, "cluster")

class DetectPekkoComponentCrashTestMultiJvmNode2 extends DetectPekkoComponentCrashTest(0, "cluster")

class DetectPekkoComponentCrashTestMultiJvmNode3 extends DetectPekkoComponentCrashTest(0, "cluster")

/**
 * This test exercises below steps :
 * 1. Registering pekko connection on member1 node
 * 2. seed(master) and member2 is tracking a pekko connection which is registered on slave (member1)
 * 3. Exiting member1 using testConductor.exit(member1, 1) (tell the remote node to shut itself down using System.exit with the given
 * exitValue 1. The node will also be removed from cluster)
 * 4. Once remote member1 is exited, we are asserting that master (seed) and member2 should receive LocationRemoved event within 5 seconds
 * => probe.requestNext(5.seconds) shouldBe a[LocationRemoved]
 *
**/
// CSW-81: Graceful removal of component
class DetectPekkoComponentCrashTest(ignore: Int, mode: String)
    extends helpers.LSNodeSpec(config = new helpers.TwoMembersAndSeed, mode)
    with Eventually
    with OptionValues {

  import config._

  implicit val patience: PatienceConfig = PatienceConfig(5.seconds, 100.millis)

  // DEOPSCSW-15: Spike jmDNS/CRDT perf
  // DEOPSCSW-35: CRDT detects comp/service crash
  // DEOPSCSW-36: Track a crashed service/comp
  test(
    s"${testPrefixWithSuite} pekko component running on one node should detect if other component running on another node crashes | DEOPSCSW-15, DEOPSCSW-35, DEOPSCSW-36, DEOPSCSW-429"
  ) {

    val pekkoConnection = PekkoConnection(ComponentId(Prefix(Subsystem.Container, "Container1"), ComponentType.Container))
    val httpConnection = HttpConnection(ComponentId(Prefix(Subsystem.Container, "Container1"), ComponentType.Container))

    runOn(seed) {

      val probe = TestProbe[TrackingEvent]("test-probe")

      val switch = locationService
        .track(pekkoConnection)
        .toMat(Sink.foreach(probe.ref.tell(_)))(Keep.left)
        .run()
      enterBarrier("Registration")

      probe.expectMessageType[LocationUpdated]
      Thread.sleep(2000)

      Await.result(testConductor.exit(member1, 0), 5.seconds)
      locationService
        .find(httpConnection)
        .await
        .value
        .connection shouldBe httpConnection
      enterBarrier("after-crash")

      // Story CSW-15 requires crash detection within 10 seconds with a goal of 5 seconds.
      // This 5.seconds demonstrates that if the test passes, the performance goal is met. Could be relaxed to 10 seconds
      // if needed.
      within(15.seconds) {
        awaitAssert {
          probe.expectMessageType[LocationRemoved](15.seconds)
        }
      }

      // at this stage, probe has received LocationRemoved event but clusters local cache might not have updated by this time
      // hence asserting in eventually block
      eventually {
        locationService.list.await.size shouldBe 1
        locationService.find(httpConnection).await shouldBe None
      }

      // clean up
      switch.cancel()
    }

    runOn(member1) {
      val system   = ActorSystemFactory.remote(SpawnProtocol(), "test")
      val actorRef = system.spawn(Behaviors.empty, "trombone-hcd-1")

      import PekkoRegistrationFactory._

      locationService
        .register(make(pekkoConnection, actorRef, Metadata(Map("key1" -> "value1"))))
        .await
      val port = 1234
      locationService.register(HttpRegistration(httpConnection, port, "")).await
      enterBarrier("Registration")

      Await.ready(system.whenTerminated, 5.seconds)
    }

    runOn(member2) {
      val port   = 9595
      val prefix = "/trombone/hcd"

      val httpConnection   = HttpConnection(models.ComponentId(Prefix(Subsystem.NFIRAOS, "Assembly1"), ComponentType.Assembly))
      val httpRegistration = HttpRegistration(httpConnection, port, prefix)
      val probe            = TestProbe[TrackingEvent]("test-probe")

      locationService.register(httpRegistration).await

      enterBarrier("Registration")
      val switch = locationService
        .track(pekkoConnection)
        .toMat(Sink.foreach(probe.ref.tell(_)))(Keep.left)
        .run()
      Thread.sleep(2000)
      enterBarrier("after-crash")

      within(15.seconds) {
        awaitAssert {
          probe.expectMessageType[LocationRemoved](15.seconds)
        }
      }

      // clean up
      switch.cancel()
    }
    enterBarrier("end")
  }
}
