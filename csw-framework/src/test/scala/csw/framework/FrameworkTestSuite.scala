package csw.framework

import akka.actor
import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.models.CswServices
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.framework.ComponentInfo
import csw.messages.{ComponentMessage, ContainerIdleMessage, TopLevelActorMessage}
import csw.services.location.commons.ActorSystemFactory
import csw.services.logging.scaladsl.LoggerFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

private[csw] abstract class FrameworkTestSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val untypedSystem: actor.ActorSystem  = ActorSystemFactory.remote()
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "testHcd")
  implicit val settings: TestKitSettings         = TestKitSettings(typedSystem)
  implicit val timeout: Timeout                  = Timeout(5.seconds)
  def frameworkTestMocks(): FrameworkTestMocks   = new FrameworkTestMocks

  override protected def afterAll(): Unit = {
    Await.result(untypedSystem.terminate(), 5.seconds)
    Await.result(typedSystem.terminate(), 5.seconds)
  }

  def getSampleHcdWiring(componentHandlers: ComponentHandlers): ComponentBehaviorFactory =
    (ctx: ActorContext[TopLevelActorMessage], componentInfo: ComponentInfo, cswServices: CswServices) => componentHandlers

  def getSampleAssemblyWiring(assemblyHandlers: ComponentHandlers): ComponentBehaviorFactory =
    (ctx: ActorContext[TopLevelActorMessage], componentInfo: ComponentInfo, cswServices: CswServices) => assemblyHandlers

  def createSupervisorAndStartTLA(
      componentInfo: ComponentInfo,
      testMocks: FrameworkTestMocks,
      containerRef: ActorRef[ContainerIdleMessage] = TestProbe[ContainerIdleMessage].ref
  ): ActorRef[ComponentMessage] = {
    import testMocks._

    val supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(containerRef),
      componentInfo,
      registrationFactory,
      new CswServices(
        cswServices.locationService,
        cswServices.eventService,
        cswServices.alarmService,
        new LoggerFactory(componentInfo.name),
        cswServices.configClientService,
        cswServices.currentStatePublisher,
        commandResponseManager
      )
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    untypedSystem.spawnAnonymous(supervisorBehavior)
  }

}
