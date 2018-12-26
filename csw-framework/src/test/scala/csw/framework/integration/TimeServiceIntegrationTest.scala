package csw.framework.integration

import akka.actor.testkit.typed.scaladsl.{TestInbox, TestProbe}
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.models.framework.ContainerLifecycleState
import csw.common.FrameworkAssertions.assertThatContainerIsRunning
import csw.common.components.framework.SampleComponentState._
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.framework.internal.wiring.{Container, FrameworkWiring}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Assembly
import csw.location.api.models.Connection.AkkaConnection
import csw.params.commands
import csw.params.commands.CommandName
import csw.params.core.states.CurrentState

import scala.concurrent.duration.DurationLong

//DEOPSCSW-550: Provide TimeService accessible to component developers
class TimeServiceIntegrationTest extends FrameworkIntegrationSuite {

  import testWiring._

  private val filterAssemblyConnection = AkkaConnection(ComponentId("Filter", Assembly))
  private val wiring                   = FrameworkWiring.make(testActorSystem)

  override def afterAll(): Unit = {
    Http(testActorSystem).shutdownAllConnectionPools().await
    super.afterAll()
  }

  test("should be able to schedule using time service") {
    val containerRef = Container.spawn(ConfigFactory.load("container_tracking_connections.conf"), wiring).await

    val assemblyProbe                = TestInbox[CurrentState]()
    val containerLifecycleStateProbe = TestProbe[ContainerLifecycleState]("container-lifecycle-state-probe")
    assertThatContainerIsRunning(containerRef, containerLifecycleStateProbe, 5.seconds)

    val filterAssemblyLocation = wiring.locationService.find(filterAssemblyConnection).await

    val assemblyCommandService = CommandServiceFactory.make(filterAssemblyLocation.get)

    assemblyCommandService.subscribeCurrentState(assemblyProbe.ref ! _)

    implicit val timeout: Timeout = Timeout(100.millis)
    assemblyCommandService.submit(commands.Setup(prefix, CommandName("time.service.scheduler.success"), None))
    Thread.sleep(1000)

    assemblyProbe.receiveAll() should contain(CurrentState(prefix, timeServiceSchedulerState))
  }
}
