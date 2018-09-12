package csw.framework

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.{actor, testkit, Done}
import csw.framework.internal.pubsub.PubSubBehaviorFactory
import csw.framework.models.CswContext
import csw.messages.CommandResponseManagerMessage
import csw.messages.commands.CommandResponse
import csw.messages.framework.{LifecycleStateChanged, PubSub}
import csw.services.location.api.models.Connection.AkkaConnection
import csw.services.location.api.javadsl.ILocationService
import csw.services.location.api.models.AkkaRegistration
import csw.services.location.api.scaladsl.LocationService
import csw.messages.params.models.{Id, Prefix}
import csw.messages.params.states.CurrentState
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.command.CommandResponseManager
import csw.services.config.api.scaladsl.ConfigClientService
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.event.EventServiceFactory
import csw.services.event.api.scaladsl.EventService
import csw.services.location.models.RegistrationResult
import csw.services.location.scaladsl.RegistrationFactory
import csw.services.logging.scaladsl.{Logger, LoggerFactory}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, _}
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

class FrameworkTestMocks(implicit untypedSystem: actor.ActorSystem, system: ActorSystem[Nothing]) extends MockitoSugar {

  ///////////////////////////////////////////////
  val testActor: ActorRef[Any]                 = testkit.TestProbe("test-probe").testActor
  val akkaRegistration                         = AkkaRegistration(mock[AkkaConnection], Prefix("nfiraos.ncc.trombone"), testActor, testActor)
  val locationService: LocationService         = mock[LocationService]
  val eventServiceFactory: EventServiceFactory = mock[EventServiceFactory]
  val eventService: EventService               = mock[EventService]
  val alarmService: AlarmService               = mock[AlarmService]
  val registrationResult: RegistrationResult   = mock[RegistrationResult]
  val registrationFactory: RegistrationFactory = mock[RegistrationFactory]

  when(registrationFactory.akkaTyped(any[AkkaConnection], any[Prefix], any[ActorRef[_]]))
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

  val lifecycleStateProbe: TestProbe[LifecycleStateChanged] = TestProbe[LifecycleStateChanged]
  val compStateProbe: TestProbe[CurrentState]               = TestProbe[CurrentState]

  ///////////////////////////////////////////////
  val loggerFactory: LoggerFactory = mock[LoggerFactory]
  val logger: Logger               = mock[Logger]

  when(loggerFactory.getLogger).thenReturn(logger)
  when(loggerFactory.getLogger(any[actor.ActorContext])).thenReturn(logger)
  when(loggerFactory.getLogger(any[ActorContext[_]])).thenReturn(logger)

  ///////////////////////////////////////////////
  val pubSubComponentActor: ActorRef[PubSub[CurrentState]] =
    untypedSystem.spawnAnonymous(new PubSubBehaviorFactory().make[CurrentState]("pub-sub-component", loggerFactory))
  val currentStatePublisher = new CurrentStatePublisher(pubSubComponentActor)

  ///////////////////////////////////////////////
  val configClientService: ConfigClientService = ConfigClientFactory.clientApi(untypedSystem, locationService)

  val cswCtx: CswContext =
    new CswContext(
      locationService,
      eventService,
      alarmService,
      loggerFactory,
      configClientService,
      currentStatePublisher,
      commandResponseManager,
      ComponentInfos.dummyInfo
    )
}
