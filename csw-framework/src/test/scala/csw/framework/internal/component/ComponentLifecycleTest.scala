package csw.framework.internal.component

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.{Behavior, PostStop}
import csw.framework.models.CswServices
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{ComponentInfos, CurrentStatePublisher, FrameworkTestSuite}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.messages.RunningMessage.Lifecycle
import csw.messages.TopLevelActorIdleMessage.Initialize
import csw.messages.commands.CommandResponse.{Accepted, Completed, Error}
import csw.messages.commands.{CommandName, CommandResponse, Observe, Setup}
import csw.messages.framework.ToComponentLifecycleMessages._
import csw.messages.params.generics.KeyType
import csw.messages.params.models.{ObsId, Prefix}
import csw.messages.{CommandResponseManagerMessage, FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.command.CommandResponseManager
import csw.services.event.api.scaladsl.EventService
import csw.services.location.scaladsl.LocationService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-177-Hooks for lifecycle management
// DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkTestSuite with MockitoSugar {

  class RunningComponent(
      supervisorProbe: TestProbe[FromComponentLifecycleMessage],
      commandStatusServiceProbe: TestProbe[CommandResponseManagerMessage]
  ) {

    val locationService: LocationService               = mock[LocationService]
    val eventService: EventService                     = mock[EventService]
    val alarmService: AlarmService                     = mock[AlarmService]
    val commandResponseManager: CommandResponseManager = mock[CommandResponseManager]
    when(commandResponseManager.commandResponseManagerActor).thenReturn(commandStatusServiceProbe.ref)

    val sampleHcdHandler: ComponentHandlers = mock[ComponentHandlers]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    when(sampleHcdHandler.onShutdown()).thenReturn(Future.unit)
    val factory = new TestComponentBehaviorFactory(sampleHcdHandler)

    val cswServices: CswServices = new CswServices(
      locationService,
      eventService,
      alarmService,
      frameworkTestMocks().loggerFactory,
      frameworkTestMocks().configClientService,
      mock[CurrentStatePublisher],
      commandResponseManager,
      ComponentInfos.hcdInfo
    )

    private val behavior: Behavior[Nothing] = factory.make(supervisorProbe.ref, cswServices)
    val componentBehaviorTestKit: BehaviorTestKit[TopLevelActorMessage] =
      BehaviorTestKit(behavior.asInstanceOf[Behavior[TopLevelActorMessage]])
    componentBehaviorTestKit.run(Initialize)
  }

  test("running component should handle RunOffline lifecycle message") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(true)

    componentBehaviorTestKit.run(Lifecycle(GoOffline))
    verify(sampleHcdHandler).onGoOffline()
    verify(sampleHcdHandler).isOnline
  }

  test("running component should not accept RunOffline lifecycle message when it is already offline") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    componentBehaviorTestKit.run(Lifecycle(GoOffline))
    verify(sampleHcdHandler, never).onGoOffline()
  }

  test("running component should handle RunOnline lifecycle message when it is Offline") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    componentBehaviorTestKit.run(Lifecycle(GoOnline))
    verify(sampleHcdHandler).onGoOnline()
  }

  test("running component should not accept RunOnline lifecycle message when it is already Online") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    when(sampleHcdHandler.isOnline).thenReturn(true)

    componentBehaviorTestKit.run(Lifecycle(GoOnline))
    verify(sampleHcdHandler, never).onGoOnline()
  }

  test("running component should clean up using onShutdown handler before stopping") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    componentBehaviorTestKit.signal(PostStop)
    verify(sampleHcdHandler).onShutdown()
  }

  test("running component should handle Submit command") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseProbe      = TestProbe[CommandResponse]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Setup(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    when(sampleHcdHandler.validateCommand(ArgumentMatchers.any[Setup]())).thenReturn(Accepted(sc1.runId))

    doNothing()
      .when(sampleHcdHandler)
      .onSubmit(ArgumentMatchers.any[Setup]())

    componentBehaviorTestKit.run(Submit(sc1, commandResponseProbe.ref))

    verify(sampleHcdHandler).validateCommand(sc1)
    verify(sampleHcdHandler).onSubmit(sc1)
    commandResponseProbe.expectMessage(Accepted(sc1.runId))
    commandStatusServiceProbe.expectMessage(AddOrUpdateCommand(sc1.runId, Accepted(sc1.runId)))
  }

  test("running component should handle Oneway command") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseProbe      = TestProbe[CommandResponse]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Observe(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    when(sampleHcdHandler.validateCommand(ArgumentMatchers.any[Setup]())).thenReturn(Accepted(sc1.runId))
    doNothing().when(sampleHcdHandler).onOneway(ArgumentMatchers.any[Setup]())

    componentBehaviorTestKit.run(Oneway(sc1, commandResponseProbe.ref))

    verify(sampleHcdHandler).validateCommand(sc1)
    verify(sampleHcdHandler).onOneway(sc1)
    commandResponseProbe.expectMessage(Accepted(sc1.runId))
    commandStatusServiceProbe.expectNoMessage(3.seconds)
  }

  //DEOPSCSW-313: Support short running actions by providing immediate response
  test("running component can send an immediate response to a submit command and avoid invoking further processing") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseProbe      = TestProbe[CommandResponse]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Setup(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    when(sampleHcdHandler.validateCommand(ArgumentMatchers.any[Setup]())).thenReturn(Completed(sc1.runId))

    doNothing()
      .when(sampleHcdHandler)
      .onSubmit(ArgumentMatchers.any[Setup]())

    componentBehaviorTestKit.run(Submit(sc1, commandResponseProbe.ref))

    verify(sampleHcdHandler).validateCommand(sc1)
    verify(sampleHcdHandler, never()).onSubmit(sc1)
    commandResponseProbe.expectMessage(Completed(sc1.runId))
    commandStatusServiceProbe.expectMessage(AddOrUpdateCommand(sc1.runId, Completed(sc1.runId)))
  }

  //DEOPSCSW-313: Support short running actions by providing immediate response
  test("running component can send an immediate response to a oneway command and avoid invoking further processing") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val commandResponseProbe      = TestProbe[CommandResponse]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Observe(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    val error = Error(sc1.runId, "error from the test command")
    when(sampleHcdHandler.validateCommand(ArgumentMatchers.any[Setup]())).thenReturn(error)
    doNothing().when(sampleHcdHandler).onOneway(ArgumentMatchers.any[Setup]())

    componentBehaviorTestKit.run(Oneway(sc1, commandResponseProbe.ref))

    verify(sampleHcdHandler).validateCommand(sc1)
    verify(sampleHcdHandler, never()).onOneway(sc1)
    commandResponseProbe.expectMessage(error)
    commandStatusServiceProbe.expectNoMessage(3.seconds)
  }

}
