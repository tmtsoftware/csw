package csw.framework.internal.component

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.{Behavior, PostStop}
import csw.command.client.MiniCRM.CRMMessage
import csw.command.client.{CommandResponseManager, MiniCRM}
import csw.command.client.messages.CommandMessage.{Oneway, Submit}
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.TopLevelActorIdleMessage.Initialize
import csw.command.client.messages.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.command.client.models.framework.ToComponentLifecycleMessage._
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{ComponentInfos, CurrentStatePublisher, FrameworkTestSuite}
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse._
import csw.params.commands.{CommandName, ControlCommand, Observe, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.{Id, ObsId}
import csw.prefix.models.Prefix
import org.mockito.captor.ArgCaptor
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

// DEOPSCSW-177-Hooks for lifecycle management
// DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkTestSuite with MockitoSugar with ArgumentMatchersSugar {

  class RunningComponent(
      supervisorProbe: TestProbe[FromComponentLifecycleMessage],
      commandStatusServiceProbe: TestProbe[MiniCRM.CRMMessage]
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

  test("running component should handle RunOffline lifecycle message | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(true)

    componentBehaviorTestKit.run(Lifecycle(GoOffline))
    verify(sampleHcdHandler).onGoOffline()
    verify(sampleHcdHandler).isOnline
  }

  test("running component should not accept RunOffline lifecycle message when it is already offline | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    componentBehaviorTestKit.run(Lifecycle(GoOffline))
    verify(sampleHcdHandler, never).onGoOffline()
  }

  test("running component should handle RunOnline lifecycle message when it is Offline | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._
    when(sampleHcdHandler.isOnline).thenReturn(false)

    componentBehaviorTestKit.run(Lifecycle(GoOnline))
    verify(sampleHcdHandler).onGoOnline()
  }

  test("running component should not accept RunOnline lifecycle message when it is already Online | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    when(sampleHcdHandler.isOnline).thenReturn(true)

    componentBehaviorTestKit.run(Lifecycle(GoOnline))
    verify(sampleHcdHandler, never).onGoOnline()
  }

  test("running component should clean up using onShutdown handler before stopping | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    componentBehaviorTestKit.signal(PostStop)
    verify(sampleHcdHandler).onShutdown()
  }

  test("running component should handle Submit command | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val submitResponseProbe       = TestProbe[SubmitResponse]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Setup(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    // Needed for verification
    val idCaptor = ArgCaptor[Id]

    // Capture the first runId passed in validateCommand
    when(sampleHcdHandler.validateCommand(idCaptor.capture, any[Setup]))
      .thenAnswer((id: Id, cc: ControlCommand) => Accepted(id))
    when(sampleHcdHandler.onSubmit(any[Id], any[Setup])).thenAnswer((id: Id, cc: ControlCommand) => Completed(id))

    componentBehaviorTestKit.run(Submit(sc1, submitResponseProbe.ref))

    // Captured when command was submitted
    val testId = idCaptor.value
    verify(sampleHcdHandler).validateCommand(testId, sc1)
    verify(sampleHcdHandler).onSubmit(testId, sc1)
    submitResponseProbe.expectMessage(Completed(testId))
  }

  test("running component should handle Oneway command | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val onewayResponseProbe       = TestProbe[OnewayResponse]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Observe(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    val idCaptor = ArgCaptor[Id]

    // A one way returns validation but is not entered into command response manager
    when(sampleHcdHandler.validateCommand(idCaptor.capture, any[Setup]))
      .thenAnswer((id: Id, cc: ControlCommand) => Accepted(id))
    doNothing.when(sampleHcdHandler).onOneway(any[Id], any[Setup])

    componentBehaviorTestKit.run(Oneway(sc1, onewayResponseProbe.ref))

    val testId = idCaptor.value
    verify(sampleHcdHandler).validateCommand(testId, sc1)
    verify(sampleHcdHandler).onOneway(testId, sc1)
    onewayResponseProbe.expectMessage(Accepted(testId))
    // Shouldn't get anything here
    onewayResponseProbe.expectNoMessage(3.seconds)
  }

  //DEOPSCSW-313: Support short running actions by providing immediate response
  test(
    "running component can send an immediate response to a submit command and avoid invoking further processing | DEOPSCSW-177, DEOPSCSW-179, DEOPSCSW-313"
  ) {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val submitResponseProbe       = TestProbe[SubmitResponse]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Setup(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    val idCaptor = ArgCaptor[Id]
    // validate returns Accepted and onSubmit returns Completed
    when(sampleHcdHandler.validateCommand(idCaptor.capture, any[Setup]))
      .thenAnswer((id: Id, cc: ControlCommand) => Accepted(id))
    when(sampleHcdHandler.onSubmit(any[Id], any[Setup])).thenAnswer((id: Id, cc: ControlCommand) => Completed(id))

    componentBehaviorTestKit.run(Submit(sc1, submitResponseProbe.ref))

    val testId = idCaptor.value
    verify(sampleHcdHandler).validateCommand(testId, sc1)
    verify(sampleHcdHandler).onSubmit(testId, sc1)
    submitResponseProbe.expectMessage(Completed(testId))
  }

  // Demonstrate oneway failure
  test("running component can send a oneway command that is rejected | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]
    val onewayResponseProbe       = TestProbe[OnewayResponse]
    val commandStatusServiceProbe = TestProbe[CRMMessage]
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent._

    val obsId: ObsId = ObsId("Obs001")
    val sc1 = Observe(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    val idCaptor = ArgCaptor[Id]

    //val invalid = Invalid(sc1.commandName, testId, OtherIssue("error from the test command"))
    when(sampleHcdHandler.validateCommand(idCaptor.capture, any[Setup]))
      .thenAnswer((id: Id, cc: ControlCommand) => Invalid(id, OtherIssue("error from the test command")))
    doNothing.when(sampleHcdHandler).onOneway(any[Id], any[Setup])

    componentBehaviorTestKit.run(Oneway(sc1, onewayResponseProbe.ref))

    val testId = idCaptor.value
    // onValidate called
    verify(sampleHcdHandler).validateCommand(testId, sc1)
    // onOneway called
    verify(sampleHcdHandler, never).onOneway(testId, sc1)
    onewayResponseProbe.expectMessage(Invalid(testId, OtherIssue("error from the test command")))
    // No contact on command response manager
    onewayResponseProbe.expectNoMessage(3.seconds)
  }
}
