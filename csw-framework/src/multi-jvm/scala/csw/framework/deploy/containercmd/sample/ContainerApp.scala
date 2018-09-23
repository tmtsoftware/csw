package csw.framework.deploy.containercmd.sample

import akka.actor.ActorSystem
import akka.actor.typed
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.{Config, ConfigFactory}
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.command.messages.ContainerMessage
import csw.command.models.framework.ContainerLifecycleState
import csw.command.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.command.messages.SupervisorContainerCommonMessages.Restart
import csw.location.api.commons.ClusterSettings

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object ContainerApp extends App {
  private val clusterSettings: ClusterSettings         = ClusterSettings().withManagementPort(5555)
  private val system: ActorSystem                      = clusterSettings.system
  implicit val actorSystem: typed.ActorSystem[Nothing] = system.toTyped
  implicit val testkit: TestKitSettings                = TestKitSettings(actorSystem)
  private val wiring                                   = FrameworkWiring.make(system)
  private val config: Config                           = ConfigFactory.load("laser_container.conf")
  private val ref: ActorRef[ContainerMessage] =
    Await.result(Container.spawn(config, wiring), 5.seconds)

  Thread.sleep(2000)

  ref ! Restart

  Thread.sleep(2000)

  private val containerLifecycleStateProbe: TestProbe[ContainerLifecycleState] = TestProbe[ContainerLifecycleState]
  ref ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)

  containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Running)
}
