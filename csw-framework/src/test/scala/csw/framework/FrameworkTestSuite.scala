package csw.framework

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.{ComponentMessage, ContainerIdleMessage, TopLevelActorMessage}
import csw.command.client.models.framework.ComponentInfo
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.models.{ComponentContext, CswContext}
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.location.client.ActorSystemFactory
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.scaladsl.LoggerFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

private[csw] abstract class FrameworkTestSuite extends AnyFunSuite with Matchers with BeforeAndAfterAll {
  implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "testHcd")
  implicit val settings: TestKitSettings                       = TestKitSettings(typedSystem)
  implicit val timeout: Timeout                                = Timeout(5.seconds)
  def frameworkTestMocks(): FrameworkTestMocks                 = new FrameworkTestMocks

  override protected def afterAll(): Unit = {
    typedSystem.terminate()
    Await.result(typedSystem.whenTerminated, 5.seconds)
  }

  def getSampleHcdWiring(componentHandlers: ComponentHandlers): ComponentBehaviorFactory =
    (_: ComponentContext[TopLevelActorMessage], _: CswContext) => componentHandlers

  def getSampleAssemblyWiring(assemblyHandlers: ComponentHandlers): ComponentBehaviorFactory =
    (_: ComponentContext[TopLevelActorMessage], _: CswContext) => assemblyHandlers

  def createSupervisorAndStartTLA(
      componentInfo: ComponentInfo,
      testMocks: FrameworkTestMocks,
      containerRef: ActorRef[ContainerIdleMessage] = TestProbe[ContainerIdleMessage]().ref
  ): ActorRef[ComponentMessage] = {
    import testMocks._

    val supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(containerRef),
      registrationFactory,
      new CswContext(
        cswCtx.locationService,
        cswCtx.eventService,
        cswCtx.alarmService,
        cswCtx.timeServiceScheduler,
        new LoggerFactory(componentInfo.prefix),
        cswCtx.configClientService,
        cswCtx.currentStatePublisher,
        commandResponseManager,
        componentInfo
      )
    )

    // it creates a supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    typedSystem.spawn(supervisorBehavior, "")
  }

}
