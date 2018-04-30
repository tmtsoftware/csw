package csw.framework

import akka.actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.testkit.typed.TestKitSettings
import akka.testkit.typed.scaladsl.TestProbe
import akka.util.Timeout
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.messages.framework.ComponentInfo
import csw.messages.scaladsl.{ComponentMessage, ContainerIdleMessage}
import csw.services.location.commons.ActorSystemFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

abstract class FrameworkTestSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val untypedSystem: actor.ActorSystem  = ActorSystemFactory.remote()
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "testHcd")
  implicit val settings: TestKitSettings         = TestKitSettings(typedSystem)
  implicit val timeout: Timeout                  = Timeout(5.seconds)
  def frameworkTestMocks(): FrameworkTestMocks   = new FrameworkTestMocks

  override protected def afterAll(): Unit = {
    Await.result(untypedSystem.terminate(), 5.seconds)
    Await.result(typedSystem.terminate(), 5.seconds)
  }

  /*  TODO is this needed? - JLW
  def getSampleHcdWiring(
      componentHandlers: ComponentHandlers
  ): ComponentBehaviorFactory =
    (ctx: ActorContext[TopLevelActorMessage],
     componentInfo: ComponentInfo,
     commandResponseManager: CommandResponseManager,
     currentStatePublisher: CurrentStatePublisher,
     locationService: LocationService,
     loggerFactory: LoggerFactory) => componentHandlers

  def getSampleAssemblyWiring(
      assemblyHandlers: ComponentHandlers
  ): ComponentBehaviorFactory =
    (ctx: ActorContext[TopLevelActorMessage],
     componentInfo: ComponentInfo,
     commandResponseManager: CommandResponseManager,
     currentStatePublisher: CurrentStatePublisher,
     locationService: LocationService,
     loggerFactory: LoggerFactory) => assemblyHandlers
   */

  def createSupervisorAndStartTLA(
      componentInfo: ComponentInfo,
      testMocks: FrameworkTestMocks,
      containerRef: ActorRef[ContainerIdleMessage] = TestProbe[ContainerIdleMessage].ref
  ): ActorRef[ComponentMessage] = {
    import testMocks._

    val supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(containerRef),
      componentInfo,
      locationService,
      registrationFactory,
      commandResponseManagerFactory
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    untypedSystem.spawnAnonymous(supervisorBehavior)
  }

}
