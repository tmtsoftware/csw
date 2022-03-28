/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.{TestInbox, TestProbe}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.models.framework.ContainerLifecycleState
import csw.common.FrameworkAssertions.assertThatContainerIsRunning
import csw.common.components.framework.SampleComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.api.models
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands
import csw.params.commands.CommandName
import csw.params.core.states.CurrentState
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.DurationLong

//DEOPSCSW-550: Provide TimeService accessible to component developers
//CSW-82: ComponentInfo should take prefix
class TimeServiceIntegrationTest extends FrameworkIntegrationSuite {

  import testWiring._

  private val filterAssemblyConnection = AkkaConnection(models.ComponentId(Prefix(Subsystem.TCS, "Filter"), Assembly))
  private val wiring                   = FrameworkWiring.make(seedActorSystem)

  override def afterAll(): Unit = {
    super.afterAll()
  }

  test("should be able to schedule using time service | DEOPSCSW-550") {
    val containerRef = Container.spawn(ConfigFactory.load("container_tracking_connections.conf"), wiring).await

    val assemblyProbe                = TestInbox[CurrentState]()
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    val filterAssemblyLocation = testWiring.seedLocationService.find(filterAssemblyConnection).await

    val assemblyCommandService = CommandServiceFactory.make(filterAssemblyLocation.get)

    assemblyCommandService.subscribeCurrentState(assemblyProbe.ref ! _)
    Thread.sleep(500)

    implicit val timeout: Timeout = Timeout(500.millis)
    assemblyCommandService.submitAndWait(commands.Setup(prefix, CommandName("time.service.scheduler.success"), None))
    Thread.sleep(1000)

    assemblyProbe.receiveAll() should contain(CurrentState(prefix, timeServiceSchedulerState))
  }
}
