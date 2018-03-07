package csw.framework

import akka.typed.scaladsl.ActorContext
import akka.typed.scaladsl.adapter._
import akka.typed.testkit.TestKitSettings
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, ActorSystem}
import akka.{actor, testkit, Done}
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.internal.supervisor.SupervisorBehavior
import csw.messages.{CommandResponseManagerMessage, SupervisorMessage}
import csw.messages.ccs.commands.CommandResponse
import csw.messages.location.Connection.AkkaConnection
import csw.messages.models.{LifecycleStateChanged, PubSub}
import csw.messages.params.models.Id
import csw.messages.params.states.CurrentState
import csw.services.command.internal.CommandResponseManagerFactory
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.javadsl.ILocationService
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class FrameworkTestMocks(
    implicit untypedSystem: actor.ActorSystem,
    system: ActorSystem[Nothing],
    settings: TestKitSettings
) extends MockitoSugar {

  ///////////////////////////////////////////////
  val testActor: ActorRef[Any]                 = testkit.TestProbe("test-probe").testActor
  val akkaRegistration                         = AkkaRegistration(mock[AkkaConnection], Some("nfiraos.ncc.trombone"), testActor, testActor)
  val locationService: LocationService         = mock[LocationService]
  val registrationResult: RegistrationResult   = mock[RegistrationResult]
  val registrationFactory: RegistrationFactory = mock[RegistrationFactory]

  when(registrationFactory.akkaTyped(any[AkkaConnection], any[ActorRef[_]]))
    .thenReturn(akkaRegistration)
  when(locationService.register(akkaRegistration)).thenReturn(Future.successful(registrationResult))
  when(locationService.unregister(any[AkkaConnection])).thenReturn(Future.successful(Done))
  when(locationService.asJava).thenReturn(mock[ILocationService])
  ///////////////////////////////////////////////

  val commandResponseManagerActor: TestProbe[CommandResponseManagerMessage] = TestProbe[CommandResponseManagerMessage]
  val commandResponseManager: CommandResponseManager                        = mock[CommandResponseManager]

  when(commandResponseManager.commandResponseManagerActor).thenReturn(commandResponseManagerActor.ref)
  doNothing().when(commandResponseManager).addOrUpdateCommand(any[Id], any[CommandResponse])

  val pubSubBehaviorFactory: PubSubBehaviorFactory                  = mock[PubSubBehaviorFactory]
  val commandResponseManagerFactory: CommandResponseManagerFactory  = mock[CommandResponseManagerFactory]
  val lifecycleStateProbe: TestProbe[PubSub[LifecycleStateChanged]] = TestProbe[PubSub[LifecycleStateChanged]]

  val compStateProbe: TestProbe[PubSub[CurrentState]] = TestProbe[PubSub[CurrentState]]
  when(
    pubSubBehaviorFactory.make[LifecycleStateChanged](
      any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.PubSubLifecycleActor),
      any[LoggerFactory]
    )
  ).thenReturn(lifecycleStateProbe.ref)
  when(
    pubSubBehaviorFactory.make[CurrentState](
      any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.PubSubComponentActor),
      any[LoggerFactory]
    )
  ).thenReturn(compStateProbe.ref)
  when(
    commandResponseManagerFactory.make(
      any[ActorContext[SupervisorMessage]],
      ArgumentMatchers.eq(SupervisorBehavior.CommandResponseManagerActorName),
      any[LoggerFactory]
    )
  ).thenReturn(commandResponseManager)

  ///////////////////////////////////////////////
  val loggerFactory: LoggerFactory = mock[LoggerFactory]
  val logger: Logger               = mock[Logger]

  when(loggerFactory.getLogger).thenReturn(logger)
  when(loggerFactory.getLogger(any[actor.ActorContext])).thenReturn(logger)
  when(loggerFactory.getLogger(any[ActorContext[_]])).thenReturn(logger)
}
