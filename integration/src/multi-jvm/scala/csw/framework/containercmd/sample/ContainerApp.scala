/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.containercmd.sample

import org.apache.pekko.actor.testkit.typed.TestKitSettings
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import com.typesafe.config.{Config, ConfigFactory}
import csw.command.client.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.command.client.messages.ContainerMessage
import csw.command.client.messages.SupervisorContainerCommonMessages.Restart
import csw.command.client.models.framework.ContainerLifecycleState
import csw.framework.internal.wiring.{Container, FrameworkWiring}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object ContainerApp {
  def main(args: Array[String]): Unit = {
    val wiring                                              = new FrameworkWiring()
    implicit val system: ActorSystem[SpawnProtocol.Command] = wiring.actorSystem
    implicit val testkit: TestKitSettings                   = TestKitSettings(system)
    val config: Config                                      = ConfigFactory.load("laser_container.conf")
    val ref: ActorRef[ContainerMessage] =
      Await.result(Container.spawn(config, wiring), 5.seconds)

    Thread.sleep(2000)

    ref ! Restart

    Thread.sleep(2000)

    val containerLifecycleStateProbe: TestProbe[ContainerLifecycleState] = TestProbe[ContainerLifecycleState]()
    ref ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)

    containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Running)
  }
}
