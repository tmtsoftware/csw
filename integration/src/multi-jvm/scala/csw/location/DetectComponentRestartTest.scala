/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.location

import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.adapter.*
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.testkit.TestProbe
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{ComponentId, ComponentType, LocationRemoved, LocationUpdated}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.server.commons.ClusterSettings
import csw.location.server.internal.{LocationServiceFactory, ServerWiring}
import csw.logging.client.commons.PekkoTypedExtension.UserActorFactory
import csw.prefix.models.{Prefix, Subsystem}
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

import scala.concurrent.Await
import scala.concurrent.duration.*
import scala.util.Try

class DetectComponentRestartTestMultiJvmNode1 extends DetectComponentRestartTest(0, "cluster")
class DetectComponentRestartTestMultiJvmNode2 extends DetectComponentRestartTest(0, "cluster")
class DetectComponentRestartTestMultiJvmNode3 extends DetectComponentRestartTest(0, "cluster")

class DetectComponentRestartTest(ignore: Int, mode: String)
    extends helpers.LSNodeSpec(config = new helpers.TwoMembersAndSeed, mode) {

  import PekkoRegistrationFactory._
  import config._

  // Fix to avoid 'java.util.concurrent.RejectedExecutionException: Worker has already been shutdown'
  InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)
  test(s"${testPrefixWithSuite} should detect re-registering of new location for a connection that has crashed/gone away | DEOPSCSW-429") {

    val pekkoConnection = PekkoConnection(ComponentId(Prefix(Subsystem.NFIRAOS, "TromboneHcd"), ComponentType.HCD))

    runOn(member1) {
      locationService.register(make(pekkoConnection, typedSystem.spawn(Behaviors.empty, "empty"))).await

      enterBarrier("location-registered")
      enterBarrier("location-updated")

      Await.result(system.whenTerminated, 10.seconds)

      startNewSystem()

      val newConfig =
        if (!sys.env.contains("CLUSTER_SEEDS"))
          config.settings.joinLocal(3552).config
        else config.settings.config

      val newSystem       = makeSystem(newConfig)
      val newTypedSystem  = newSystem.toTyped.asInstanceOf[ActorSystem[SpawnProtocol.Command]]
      val clusterSettings = ClusterSettings.make(newTypedSystem)

      val freshLocationService = mode match {
        case "http" =>
          Try(ServerWiring.make(clusterSettings, enableAuth = false).locationHttpService.start().await) match {
            case _ => // ignore binding errors
          }
          HttpLocationServiceFactory.makeLocalClient(newTypedSystem)
        case "cluster" => LocationServiceFactory.make(clusterSettings)
      }

      Thread.sleep(20000)

      freshLocationService.register(make(pekkoConnection, newTypedSystem.spawn(Behaviors.empty, "empty"))).await
      enterBarrier("member-re-registered")
    }

    runOn(seed, member2) {
      enterBarrier("location-registered")
      val testProbe  = TestProbe()
      val killSwitch = locationService.subscribe(pekkoConnection, testProbe.testActor ! _)

      testProbe.expectMsgType[LocationUpdated]
      enterBarrier("location-updated")

      runOn(seed) {
        Await.result(testConductor.shutdown(member1), 10.seconds)
      }

      testProbe.expectMsgType[LocationRemoved](25.seconds)
      Thread.sleep(2000)
      enterBarrier("member-re-registered")
      testProbe.expectMsgType[LocationUpdated](5.seconds)

      killSwitch.cancel()
    }

    enterBarrier("after-2")
  }

}
