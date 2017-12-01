package csw.framework

import akka.actor
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.common.components.framework.ComponentDomainMessage
import csw.framework.internal.supervisor.SupervisorBehaviorFactory
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.messages.framework.ComponentInfo
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage, ContainerIdleMessage, SupervisorExternalMessage}
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
      componentHandlers: ComponentHandlers[ComponentDomainMessage]
  ): ComponentBehaviorFactory[ComponentDomainMessage] =
    new ComponentBehaviorFactory[ComponentDomainMessage] {

      override def handlers(
          ctx: ActorContext[ComponentMessage],
          componentInfo: ComponentInfo,
          commandResponseManager: ActorRef[CommandResponseManagerMessage],
          pubSubRef: ActorRef[PublisherMessage[CurrentState]],
          locationService: LocationService,
          loggerFactory: LoggerFactory
      ): ComponentHandlers[ComponentDomainMessage] =
        componentHandlers
    }

  def getSampleAssemblyWiring(
      assemblyHandlers: ComponentHandlers[ComponentDomainMessage]
  ): ComponentBehaviorFactory[ComponentDomainMessage] =
    new ComponentBehaviorFactory[ComponentDomainMessage] {
      override def handlers(
          ctx: ActorContext[ComponentMessage],
          componentInfo: ComponentInfo,
          commandResponseManager: ActorRef[CommandResponseManagerMessage],
          pubSubRef: ActorRef[PublisherMessage[CurrentState]],
          locationService: LocationService,
          loggerFactory: LoggerFactory
      ): ComponentHandlers[ComponentDomainMessage] =
        assemblyHandlers
    }

  def createSupervisorAndStartTLA(
      componentInfo: ComponentInfo,
      testMocks: FrameworkTestMocks,
      containerRef: ActorRef[ContainerIdleMessage] = TestProbe[ContainerIdleMessage].ref
  ): ActorRef[SupervisorExternalMessage] = {
    import testMocks._

    val supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(containerRef),
      componentInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    untypedSystem.spawnAnonymous(supervisorBehavior)
  }

}
