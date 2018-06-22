package csw.framework

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.{actor, testkit, Done}
import csw.messages.commands.CommandResponse
import csw.messages.framework.LifecycleStateChanged
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.models.Id
import csw.messages.params.states.CurrentState
import csw.messages.scaladsl.{CommandResponseManagerMessage, SupervisorMessage}
import csw.services.command.internal.CommandResponseManagerFactory
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.event.internal.commons.EventServiceFactory
import csw.services.event.scaladsl.EventService
import csw.services.location.javadsl.ILocationService
import csw.services.location.models.{AkkaRegistration, RegistrationResult}
import csw.services.location.scaladsl.{LocationService, RegistrationFactory}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class FrameworkTestMocks(implicit untypedSystem: actor.ActorSystem, system: ActorSystem[Nothing]) extends MockitoSugar {

  ///////////////////////////////////////////////
  val testActor: ActorRef[Any]                 = testkit.TestProbe("test-probe").testActor
  val akkaRegistration                         = AkkaRegistration(mock[AkkaConnection], Some("nfiraos.ncc.trombone"), testActor, testActor)
  val locationService: LocationService         = mock[LocationService]
  val eventServiceFactory: EventServiceFactory = mock[EventServiceFactory]
  val eventService: EventService               = mock[EventService]
  val registrationResult: RegistrationResult   = mock[RegistrationResult]
  val registrationFactory: RegistrationFactory = mock[RegistrationFactory]

  when(registrationFactory.akkaTyped(any[AkkaConnection], any[String], any[ActorRef[_]]))
    .thenReturn(akkaRegistration)
  when(locationService.register(akkaRegistration)).thenReturn(Future.successful(registrationResult))
  when(locationService.unregister(any[AkkaConnection])).thenReturn(Future.successful(Done))
  when(locationService.asJava).thenReturn(mock[ILocationService])
  when(eventServiceFactory.make(any[LocationService]())(any[actor.ActorSystem]())).thenReturn(eventService)
  when(eventService.executionContext).thenReturn(untypedSystem.dispatcher)
  ///////////////////////////////////////////////

  val commandResponseManagerActor: TestProbe[CommandResponseManagerMessage] = TestProbe[CommandResponseManagerMessage]
  val commandResponseManager: CommandResponseManager                        = mock[CommandResponseManager]

  when(commandResponseManager.commandResponseManagerActor).thenReturn(commandResponseManagerActor.ref)
  doNothing().when(commandResponseManager).addOrUpdateCommand(any[Id], any[CommandResponse])

  val commandResponseManagerFactory: CommandResponseManagerFactory = mock[CommandResponseManagerFactory]
  val lifecycleStateProbe: TestProbe[LifecycleStateChanged]        = TestProbe[LifecycleStateChanged]
  val compStateProbe: TestProbe[CurrentState]                      = TestProbe[CurrentState]

  when(
    commandResponseManagerFactory.make(
      any[ActorContext[SupervisorMessage]],
      any[ActorRef[CommandResponseManagerMessage]]
    )
  ).thenReturn(commandResponseManager)

  ///////////////////////////////////////////////
  val loggerFactory: LoggerFactory = mock[LoggerFactory]
  val logger: Logger               = mock[Logger]

  when(loggerFactory.getLogger).thenReturn(logger)
  when(loggerFactory.getLogger(any[actor.ActorContext])).thenReturn(logger)
  when(loggerFactory.getLogger(any[ActorContext[_]])).thenReturn(logger)
}
