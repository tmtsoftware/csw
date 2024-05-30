/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import org.apache.pekko.{Done, actor}
import csw.alarm.api.scaladsl.AlarmService
import csw.command.client.models.framework.{LifecycleStateChanged, PubSub}
import csw.command.client.{CommandResponseManager, MiniCRM}
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.framework.internal.pubsub.PubSubBehavior
import csw.framework.models.CswContext
import csw.framework.scaladsl.RegistrationFactory
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoRegistration, HttpRegistration, Metadata}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.commons.PekkoTypedExtension.UserActorFactory
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.core.states.CurrentState
import csw.time.scheduler.api.TimeServiceScheduler
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, when}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class FrameworkTestMocks(implicit system: ActorSystem[SpawnProtocol.Command]) extends MockitoSugar {

  ///////////////////////////////////////////////
  val testActor: ActorRef[Any]                   = TestProbe("test-probe").ref
  val pekkoRegistration: PekkoRegistration       = PekkoRegistrationFactory.make(mock[PekkoConnection], testActor)
  val locationService: LocationService           = mock[LocationService]
  val eventServiceFactory: EventServiceFactory   = mock[EventServiceFactory]
  val eventService: EventService                 = mock[EventService]
  val alarmService: AlarmService                 = mock[AlarmService]
  val timeServiceScheduler: TimeServiceScheduler = mock[TimeServiceScheduler]
  val registrationResult: RegistrationResult     = mock[RegistrationResult]
  val registrationFactory: RegistrationFactory   = mock[RegistrationFactory]

  when(registrationFactory.pekkoTyped(any[PekkoConnection], any[ActorRef[?]], any[Metadata])).thenReturn(pekkoRegistration)
  when(locationService.register(pekkoRegistration)).thenReturn(Future.successful(registrationResult))
  when(locationService.register(any[HttpRegistration])).thenReturn(Future.successful(registrationResult))
  when(locationService.unregister(any[PekkoConnection])).thenReturn(Future.successful(Done))
  when(eventServiceFactory.make(any[LocationService])(any[ActorSystem[?]])).thenReturn(eventService)
  ///////////////////////////////////////////////

  val lifecycleStateProbe: TestProbe[LifecycleStateChanged] = TestProbe[LifecycleStateChanged]()
  val compStateProbe: TestProbe[CurrentState]               = TestProbe[CurrentState]()

  ///////////////////////////////////////////////
  val loggerFactory: LoggerFactory = mock[LoggerFactory]
  val logger: Logger               = mock[Logger]

  when(loggerFactory.getLogger).thenReturn(logger)
  when(loggerFactory.getLogger(any[actor.ActorContext])).thenReturn(logger)
  when(loggerFactory.getLogger(any[ActorContext[?]])).thenReturn(logger)

  ///////////////////////////////////////////////
  val pubSubComponentActor: ActorRef[PubSub[CurrentState]] =
    system.spawn(PubSubBehavior.make[CurrentState](loggerFactory), "pub-sub")
  val currentStatePublisher: CurrentStatePublisher = new CurrentStatePublisher(pubSubComponentActor)

  val commandResponseManagerActor: TestProbe[MiniCRM.CRMMessage] = TestProbe[MiniCRM.CRMMessage]()
  val commandResponseManager: CommandResponseManager             = mock[CommandResponseManager]

  when(commandResponseManager.commandResponseManagerActor).thenReturn(commandResponseManagerActor.ref)
  doNothing().when(commandResponseManager).updateCommand(any[SubmitResponse])

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
