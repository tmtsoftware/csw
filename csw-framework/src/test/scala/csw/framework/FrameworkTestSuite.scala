package csw.framework

import akka.actor
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers, CurrentStatePublisher}
import csw.messages.framework.ComponentInfo
import csw.messages.{ComponentMessage, ContainerIdleMessage, TopLevelActorMessage}
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.commons.ActorSystemFactory
import csw.services.location.scaladsl.LocationService
import csw.services.logging.scaladsl.LoggerFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

abstract class FrameworkTestSuite extends FunSuite with Matchers with BeforeAndAfterAll {
  implicit val untypedSystem: actor.ActorSystem = ActorSystemFactory.remote()
  implicit val system: ActorSystem[Nothing]     = ActorSystem(Actor.empty, "testHcd")
  implicit val settings: TestKitSettings        = TestKitSettings(system)
  implicit val timeout: Timeout                 = Timeout(5.seconds)
  def frameworkTestMocks(): FrameworkTestMocks  = new FrameworkTestMocks

  override protected def afterAll(): Unit = {
    Await.result(untypedSystem.terminate(), 5.seconds)
    Await.result(system.terminate(), 5.seconds)
  }

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
      pubSubBehaviorFactory,
      commandResponseManagerFactory
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    untypedSystem.spawnAnonymous(supervisorBehavior)
  }

}
