package csw.framework.internal.component

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.{Behavior, PostStop}
import csw.command.client.CommandResponseManager
import csw.command.client.messages.CommandMessage.{Oneway, Submit}
import csw.command.client.messages.CommandResponseManagerMessage.AddOrUpdateCommand
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.TopLevelActorIdleMessage.Initialize
import csw.command.client.messages.{CommandResponseManagerMessage, FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.command.client.models.framework.ToComponentLifecycleMessages._
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{ComponentInfos, CurrentStatePublisher, FrameworkTestSuite}
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.{ObsId, Prefix}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-177-Hooks for lifecycle management
// DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkTestSuite with MockitoSugar with ArgumentMatchersSugar {

  class RunningComponent(
      supervisorProbe: TestProbe[FromComponentLifecycleMessage],
      commandStatusServiceProbe: TestProbe[CommandResponseManagerMessage]
  ) {

    val commandResponseManager: CommandResponseManager = mock[CommandResponseManager]
    when(commandResponseManager.commandResponseManagerActor).thenReturn(commandStatusServiceProbe.ref)

    val sampleHcdHandler: ComponentHandlers = mock[ComponentHandlers]
    when(sampleHcdHandler.initialize()).thenReturn(Future.unit)
    when(sampleHcdHandler.onShutdown()).thenReturn(Future.unit)
    val factory = new TestComponentBehaviorFactory(sampleHcdHandler)

    val cswCtx: CswContext = new CswContext(
      frameworkTestMocks().locationService,
      frameworkTestMocks().eventService,
      frameworkTestMocks().alarmService,
      frameworkTestMocks().timeServiceScheduler,
      frameworkTestMocks().loggerFactory,
      frameworkTestMocks().configClientService,
      mock[CurrentStatePublisher],
      commandResponseManager,
      ComponentInfos.hcdInfo
    )

    private val behavior: Behavior[Nothing] = factory.make(supervisorProbe.ref, cswCtx)
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
    val submitResponseProbe       = TestProbe[SubmitResponse]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Setup(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    when(sampleHcdHandler.validateCommand(any[Setup])).thenReturn(Accepted(sc1.runId))
    when(sampleHcdHandler.onSubmit(any[Setup])).thenReturn(Completed(sc1.runId))

    componentBehaviorTestKit.run(Submit(sc1, submitResponseProbe.ref))

    verify(sampleHcdHandler).validateCommand(sc1)
    verify(sampleHcdHandler).onSubmit(sc1)
    submitResponseProbe.expectMessage(Completed(sc1.runId))
    // First receives a Started and then Completed
    commandStatusServiceProbe.expectMessage(AddOrUpdateCommand(Started(sc1.runId)))
    commandStatusServiceProbe.expectMessage(AddOrUpdateCommand(Completed(sc1.runId)))
  }

  test("running component should handle Oneway command") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val onewayResponseProbe       = TestProbe[OnewayResponse]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Observe(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))
    // A one way returns validation but is not entered into command response manager
    when(sampleHcdHandler.validateCommand(any[Setup])).thenReturn(Accepted(sc1.runId))
    doNothing.when(sampleHcdHandler).onOneway(any[Setup])

    componentBehaviorTestKit.run(Oneway(sc1, onewayResponseProbe.ref))

    verify(sampleHcdHandler).validateCommand(sc1)
    verify(sampleHcdHandler).onOneway(sc1)
    onewayResponseProbe.expectMessage(Accepted(sc1.runId))
    commandStatusServiceProbe.expectNoMessage(3.seconds)
  }

  //DEOPSCSW-313: Support short running actions by providing immediate response
  test("running component can send an immediate response to a submit command and avoid invoking further processing") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val submitResponseProbe       = TestProbe[SubmitResponse]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Setup(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))
    // validate returns Accepted and onSubmit returns Completed
    when(sampleHcdHandler.validateCommand(any[Setup])).thenReturn(Accepted(sc1.runId))
    when(sampleHcdHandler.onSubmit(any[Setup])).thenReturn(Completed(sc1.runId))

    componentBehaviorTestKit.run(Submit(sc1, submitResponseProbe.ref))

    verify(sampleHcdHandler).validateCommand(sc1)
    verify(sampleHcdHandler).onSubmit(sc1)
    submitResponseProbe.expectMessage(Completed(sc1.runId))
    // Started is received from ComponentBehavior onSubmit
    commandStatusServiceProbe.expectMessage(AddOrUpdateCommand(Started(sc1.runId)))
    commandStatusServiceProbe.expectMessage(AddOrUpdateCommand(Completed(sc1.runId)))
  }

  // Demonstrate oneway failure
  test("running component can send a oneway command that is rejected") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CommandResponseManagerMessage]
    val onewayResponseProbe       = TestProbe[OnewayResponse]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Observe(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    val invalid = Invalid(sc1.runId, OtherIssue("error from the test command"))
    when(sampleHcdHandler.validateCommand(any[Setup])).thenReturn(invalid)
    doNothing.when(sampleHcdHandler).onOneway(any[Setup])

    componentBehaviorTestKit.run(Oneway(sc1, onewayResponseProbe.ref))

    // onValidate called
    verify(sampleHcdHandler).validateCommand(sc1)
    // onOneway called
    verify(sampleHcdHandler, never).onOneway(sc1)
    onewayResponseProbe.expectMessage(invalid)
    // No contact on command response manager
    commandStatusServiceProbe.expectNoMessage(3.seconds)
  }

}
