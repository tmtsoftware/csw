package csw.framework.deploy.containercmd.sample

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import com.typesafe.config.{Config, ConfigFactory}
import csw.command.client.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.command.client.messages.ContainerMessage
import csw.command.client.messages.SupervisorContainerCommonMessages.Restart
import csw.command.client.models.framework.ContainerLifecycleState
import csw.framework.internal.wiring.{Container, FrameworkWiring}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object ContainerApp extends App {
  private val wiring                              = new FrameworkWiring()
  implicit val system: typed.ActorSystem[Nothing] = wiring.actorSystem.toTyped
  implicit val testkit: TestKitSettings           = TestKitSettings(system)
  private val config: Config                      = ConfigFactory.load("laser_container.conf")
  private val ref: ActorRef[ContainerMessage] =
    Await.result(Container.spawn(config, wiring), 5.seconds)

  Thread.sleep(2000)

  ref ! Restart

  Thread.sleep(2000)

  private val containerLifecycleStateProbe: TestProbe[ContainerLifecycleState] = TestProbe[ContainerLifecycleState]
  ref ! GetContainerLifecycleState(containerLifecycleStateProbe.ref)

  containerLifecycleStateProbe.expectMessage(ContainerLifecycleState.Running)
}
