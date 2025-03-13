/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.component

import org.apache.pekko.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import org.apache.pekko.actor.typed.{Behavior, PostStop}
import csw.command.client.MiniCRM.CRMMessage
import csw.command.client.messages.CommandMessage.{Oneway, Submit}
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.TopLevelActorIdleMessage.Initialize
import csw.command.client.messages.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.command.client.models.framework.ToComponentLifecycleMessage.*
import csw.command.client.{CommandResponseManager, MiniCRM}
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentHandlers, ComponentHandlersFactory}
import csw.framework.{ComponentInfos, CurrentStatePublisher, FrameworkTestSuite}
import csw.params.commands.CommandIssue.OtherIssue
import csw.params.commands.CommandResponse.*
import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.generics.KeyType
import csw.params.core.models.{Id, ObsId}
import csw.prefix.models.Prefix
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doNothing, verify, when}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationDouble

// DEOPSCSW-177-Hooks for lifecycle management
// DEOPSCSW-179-Unique Action for a component
class ComponentLifecycleTest extends FrameworkTestSuite with MockitoSugar {

  class RunningComponent(
      supervisorProbe: TestProbe[FromComponentLifecycleMessage],
      commandStatusServiceProbe: TestProbe[MiniCRM.CRMMessage]
  ) {
    val commandResponseManager: CommandResponseManager = mock[CommandResponseManager]
    when(commandResponseManager.commandResponseManagerActor).thenReturn(commandStatusServiceProbe.ref)

    val sampleHcdHandler: ComponentHandlers = mock[ComponentHandlers]
    val factory: ComponentHandlersFactory   = (ctx, cswCtx) => sampleHcdHandler

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

  test("running component should handle GoOffline lifecycle message when it is Online | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    componentBehaviorTestKit.run(Lifecycle(GoOffline))
    verify(sampleHcdHandler).onGoOffline()
  }

  test(
    "running component should accept GoOffline lifecycle message when it is already offline | DEOPSCSW-177, DEOPSCSW-179, CSW-102"
  ) {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    componentBehaviorTestKit.run(Lifecycle(GoOffline))
    verify(sampleHcdHandler).onGoOffline()
  }

  test("running component should handle GoOnline lifecycle message when it is Offline | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    componentBehaviorTestKit.run(Lifecycle(GoOnline))
    verify(sampleHcdHandler).onGoOnline()
  }

  test(
    "running component should accept GoOnline lifecycle message when it is already Online | DEOPSCSW-177, DEOPSCSW-179, CSW-102"
  ) {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    componentBehaviorTestKit.run(Lifecycle(GoOnline))
    verify(sampleHcdHandler).onGoOnline()
  }

  test("running component should clean up using onShutdown handler before stopping | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    componentBehaviorTestKit.signal(PostStop)
    verify(sampleHcdHandler).onShutdown()
  }

  test("running component should handle Submit command | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val submitResponseProbe       = TestProbe[SubmitResponse]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    val obsId: ObsId = ObsId("2020A-001-123")
    val sc1 = Setup(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    // Needed for verification
    val idCaptor = ArgumentCaptor.forClass(classOf[Id])

    // Capture the first runId passed in validateCommand
    when(sampleHcdHandler.validateCommand(idCaptor.capture(), any[Setup]))
      .thenAnswer(arg => Accepted(arg.getArgument(0, classOf[Id])))
    when(sampleHcdHandler.onSubmit(any[Id], any[Setup])).thenAnswer(arg => Completed(arg.getArgument(0, classOf[Id])))

    componentBehaviorTestKit.run(Submit(sc1, submitResponseProbe.ref))

    // Captured when command was submitted
    val testId = idCaptor.getValue
    verify(sampleHcdHandler).validateCommand(testId, sc1)
    verify(sampleHcdHandler).onSubmit(testId, sc1)
    submitResponseProbe.expectMessage(Completed(testId))
  }

  test("running component should handle Oneway command | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val onewayResponseProbe       = TestProbe[OnewayResponse]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    val obsId: ObsId = ObsId("2020A-001-123")
    val sc1 = Observe(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    val idCaptor = ArgumentCaptor.forClass(classOf[Id])

    // A one way returns validation but is not entered into command response manager
    when(sampleHcdHandler.validateCommand(idCaptor.capture(), any[Setup]))
      .thenAnswer(arg => Accepted(arg.getArgument(0, classOf[Id])))
    doNothing().when(sampleHcdHandler).onOneway(any[Id], any[Setup])

    componentBehaviorTestKit.run(Oneway(sc1, onewayResponseProbe.ref))

    val testId = idCaptor.getValue
    verify(sampleHcdHandler).validateCommand(testId, sc1)
    verify(sampleHcdHandler).onOneway(testId, sc1)
    onewayResponseProbe.expectMessage(Accepted(testId))
    // Shouldn't get anything here
    onewayResponseProbe.expectNoMessage(3.seconds)
  }

  // DEOPSCSW-313: Support short running actions by providing immediate response
  test(
    "running component can send an immediate response to a submit command and avoid invoking further processing | DEOPSCSW-177, DEOPSCSW-179, DEOPSCSW-313"
  ) {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val submitResponseProbe       = TestProbe[SubmitResponse]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    val obsId: ObsId = ObsId("2020A-001-123")
    val sc1 = Setup(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    val idCaptor = ArgumentCaptor.forClass(classOf[Id])
    // validate returns Accepted and onSubmit returns Completed
    when(sampleHcdHandler.validateCommand(idCaptor.capture, any[Setup]))
      .thenAnswer(arg => Accepted(arg.getArgument(0, classOf[Id])))
    when(sampleHcdHandler.onSubmit(any[Id], any[Setup])).thenAnswer(arg => Completed(arg.getArgument(0, classOf[Id])))

    componentBehaviorTestKit.run(Submit(sc1, submitResponseProbe.ref))

    val testId = idCaptor.getValue
    verify(sampleHcdHandler).validateCommand(testId, sc1)
    verify(sampleHcdHandler).onSubmit(testId, sc1)
    submitResponseProbe.expectMessage(Completed(testId))
  }

  // Demonstrate oneway failure
  test("running component can send a oneway command that is rejected | DEOPSCSW-177, DEOPSCSW-179") {
    val supervisorProbe           = TestProbe[FromComponentLifecycleMessage]()
    val onewayResponseProbe       = TestProbe[OnewayResponse]()
    val commandStatusServiceProbe = TestProbe[CRMMessage]()
    val runningComponent          = new RunningComponent(supervisorProbe, commandStatusServiceProbe)
    import runningComponent.*

    val obsId: ObsId = ObsId("2020A-001-123")
    val sc1 = Observe(Prefix("wfos.prog.cloudcover"), CommandName("wfos.prog.cloudcover"), Some(obsId))
      .add(KeyType.IntKey.make("encoder").set(22))

    val idCaptor = ArgumentCaptor.forClass(classOf[Id])

    // val invalid = Invalid(sc1.commandName, testId, OtherIssue("error from the test command"))
    when(sampleHcdHandler.validateCommand(idCaptor.capture(), any[Setup]))
      .thenAnswer(arg => Invalid(arg.getArgument(0, classOf[Id]), OtherIssue("error from the test command")))
    doNothing.when(sampleHcdHandler).onOneway(any[Id], any[Setup])

    componentBehaviorTestKit.run(Oneway(sc1, onewayResponseProbe.ref))

    val testId = idCaptor.getValue
    // onValidate called
    verify(sampleHcdHandler).validateCommand(testId, sc1)
    // onOneway called
    verify(sampleHcdHandler, Mockito.never()).onOneway(testId, sc1)
    onewayResponseProbe.expectMessage(Invalid(testId, OtherIssue("error from the test command")))
    // No contact on command response manager
    onewayResponseProbe.expectNoMessage(3.seconds)
  }
}
