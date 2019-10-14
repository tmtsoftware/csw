package csw.framework

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.{Done, actor}
import csw.alarm.api.scaladsl.AlarmService
import csw.command.client.CommandResponseManager
import csw.command.client.models.framework.{LifecycleStateChanged, PubSub}
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.framework.internal.pubsub.PubSubBehavior
import csw.framework.models.CswContext
import csw.framework.scaladsl.RegistrationFactory
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.AkkaRegistration
import csw.location.models.Connection.AkkaConnection
import csw.logging.api.scaladsl.Logger
import csw.logging.client.commons.AkkaTypedExtension.UserActorFactory
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.core.models.Prefix
import csw.params.core.states.CurrentState
import csw.time.scheduler.api.TimeServiceScheduler
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import scala.concurrent.Future

class FrameworkTestMocks(implicit system: ActorSystem[SpawnProtocol]) extends MockitoSugar with ArgumentMatchersSugar {

  ///////////////////////////////////////////////
  val testActor: ActorRef[Any] = TestProbe("test-probe").ref
  val akkaRegistration: AkkaRegistration =
    AkkaRegistrationFactory.make(mock[AkkaConnection], Prefix("nfiraos.ncc.trombone"), testActor.toURI)
  val locationService: LocationService           = mock[LocationService]
  val eventServiceFactory: EventServiceFactory   = mock[EventServiceFactory]
  val eventService: EventService                 = mock[EventService]
  val alarmService: AlarmService                 = mock[AlarmService]
  val timeServiceScheduler: TimeServiceScheduler = mock[TimeServiceScheduler]
  val registrationResult: RegistrationResult     = mock[RegistrationResult]
  val registrationFactory: RegistrationFactory   = mock[RegistrationFactory]

  when(registrationFactory.akkaTyped(any[AkkaConnection], any[Prefix], any[ActorRef[_]]))
    .thenReturn(akkaRegistration)
  when(locationService.register(akkaRegistration)).thenReturn(Future.successful(registrationResult))
  when(locationService.unregister(any[AkkaConnection])).thenReturn(Future.successful(Done))
  when(eventServiceFactory.make(any[LocationService])(any[ActorSystem[_]])).thenReturn(eventService)
  when(eventService.executionContext).thenReturn(system.executionContext)
  ///////////////////////////////////////////////

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
    system.spawn(PubSubBehavior.make[CurrentState](loggerFactory), "pub-sub")
  val currentStatePublisher: CurrentStatePublisher = new CurrentStatePublisher(pubSubComponentActor)

  val pubSubCommandEventActor: ActorRef[PubSub[SubmitResponse]] =
    system.spawn(PubSubBehavior.make[SubmitResponse](loggerFactory), name = "pub-sub-update")

  val commandResponseManager: CommandResponseManager = mock[CommandResponseManager]

  doNothing.when(commandResponseManager).updateCommand(any[SubmitResponse])

  ///////////////////////////////////////////////
  val configClientService: ConfigClientService = ConfigClientFactory.clientApi(system, locationService)

  val cswCtx: CswContext =
    new CswContext(
      locationService,
      eventService,
      alarmService,
      timeServiceScheduler,
      loggerFactory,
      configClientService,
      currentStatePublisher,
      commandResponseManager,
      ComponentInfos.dummyInfo
    )
}
