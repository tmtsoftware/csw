package csw.framework.internal.supervisor

import akka.testkit.typed.scaladsl.TestProbe
import csw.framework.ComponentInfos._
import csw.framework.FrameworkTestSuite
import csw.framework.javadsl.commons.JComponentInfos.{jHcdInfo, jHcdInfoWithInitializeTimeout}
import csw.messages.commands.CommandResponse.{Accepted, Invalid}
import csw.messages.commands._
import csw.messages.commands.matchers.DemandMatcher
import csw.messages.framework.PubSub.Subscribe
import csw.messages.framework.ToComponentLifecycleMessages.{GoOffline, GoOnline}
import csw.messages.framework.{ComponentInfo, LifecycleStateChanged, SupervisorLifecycleState}
import csw.messages.location.ComponentType.{Assembly, HCD}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.ObsId
import csw.messages.params.states.{CurrentState, DemandState, StateName}
import csw.messages.scaladsl.CommandMessage.{Oneway, Submit}
import csw.messages.scaladsl.ComponentCommonMessage.{
  ComponentStateSubscription,
  GetSupervisorLifecycleState,
  LifecycleStateSubscription
}
import csw.messages.scaladsl.ContainerIdleMessage
import csw.messages.scaladsl.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.messages.scaladsl.RunningMessage.Lifecycle
import csw.messages.scaladsl.SupervisorContainerCommonMessages.Restart
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table

import scala.concurrent.duration.DurationInt

/**
 * This tests exercises component handlers written in both scala and java
 */
// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
// DEOPSCSW-165: CSW Assembly Creation
// DEOPSCSW-166: CSW HCD Creation
// DEOPSCSW-176: Provide Infrastructure to manage TMT lifecycle
// DEOPSCSW-177: Hooks for lifecycle management
class SupervisorModuleTest extends FrameworkTestSuite with BeforeAndAfterEach {
  import csw.common.components.framework.SampleComponentState._

  val testData = Table(
    "componentInfo",
    hcdInfo,
    jHcdInfo,
  )

  test("onInitialized and onRun hooks of comp handlers should be invoked when supervisor creates comp") {

    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        val containerIdleMessageProbe = TestProbe[ContainerIdleMessage]
        val supervisorRef             = createSupervisorAndStartTLA(info, mocks, containerIdleMessageProbe.ref)

        supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))

        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        containerIdleMessageProbe.expectMessage(SupervisorLifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        verify(locationService).register(akkaRegistration)
      }
    }
  }

  // DEOPSCSW-198: Enable Assembly to accept a Setup or an Observe configuration
  // DEOPSCSW-199: Enable HCD to accept a Setup configuration
  // DEOPSCSW-200: Send Submit Configuration command
  // DEOPSCSW-201: Destination component to receive a submit command
  // DEOPSCSW-203: Write component-specific verification code
  // DEOPSCSW-204: Sender to know that Submit configuration command's validation was successful
  // DEOPSCSW-213: Sender to know that oneway configuration command's validation was successful
  // DEOPSCSW-293: Sanitise handlers in Component Handlers
  // DEOPSCSW-306: Include runId in Command response
  test("onSubmit hook should be invoked and command validation should be successful on receiving Setup config") {
    val testData = Table(
      "componentInfo",
      hcdInfo,
      assemblyInfo,
      jHcdInfo
    )

    // This proves that data used in this test contains HCD and Assembly ComponentType
    testData.find(info ⇒ info.componentType == HCD && info.name == "SampleHcd") shouldBe Some(hcdInfo)
    testData.find(info ⇒ info.componentType == HCD && info.name == "JSampleHcd") shouldBe Some(jHcdInfo)
    testData.find(info ⇒ info.componentType == Assembly && info.name == "SampleAssembly") shouldBe Some(assemblyInfo)

    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        val supervisorRef                                              = createSupervisorAndStartTLA(info, mocks)
        val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]

        supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))
        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

        val obsId: ObsId          = ObsId("Obs001")
        val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
        val setup: Setup          = Setup(prefix, CommandName("move.success"), Some(obsId), Set(param))

        supervisorRef ! Submit(setup, commandValidationResponseProbe.ref)

        // verify that validateSubmit handler is invoked
        val submitSetupValidationCurrentState = compStateProbe.expectMessageType[CurrentState]
        val submitSetupValidationDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice)))
        DemandMatcher(submitSetupValidationDemandState, timeout = 5.seconds)
          .check(submitSetupValidationCurrentState) shouldBe true
        commandValidationResponseProbe.expectMessage(Accepted(setup.runId))

        // verify that onSubmit handler is invoked
        val submitSetupCommandCurrentState = compStateProbe.expectMessageType[CurrentState]
        val submitSetupCommandDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(submitCommandChoice)))
        DemandMatcher(submitSetupCommandDemandState, timeout = 5.seconds)
          .check(submitSetupCommandCurrentState) shouldBe true

        // verify that setup config is received by handler and provide check that data is transferred
        val submitSetupConfigCurrentState = compStateProbe.expectMessageType[CurrentState]
        val submitSetupConfigDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(setupConfigChoice), param))
        DemandMatcher(submitSetupConfigDemandState, timeout = 5.seconds)
          .check(submitSetupConfigCurrentState) shouldBe true

        val observe: Observe = Observe(prefix, CommandName("move.success"), Some(obsId), Set(param))

        supervisorRef ! Submit(observe, commandValidationResponseProbe.ref)

        // verify that validateSubmit handler is invoked
        val submitValidationCurrentState = compStateProbe.expectMessageType[CurrentState]
        val submitValidationDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice)))
        DemandMatcher(submitValidationDemandState, timeout = 5.seconds)
          .check(submitValidationCurrentState) shouldBe true
        commandValidationResponseProbe.expectMessage(Accepted(observe.runId))

        // verify that onSubmit handler is invoked
        val submitCommandCurrentState = compStateProbe.expectMessageType[CurrentState]
        val submitCommandDemandState  = DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(submitCommandChoice)))
        DemandMatcher(submitCommandDemandState, timeout = 5.seconds).check(submitCommandCurrentState) shouldBe true

        // verify that observe config is received by handler and provide check that data is transferred
        val submitObserveConfigCurrentState = compStateProbe.expectMessageType[CurrentState]
        val submitObserveConfigDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(observeConfigChoice), param))
        DemandMatcher(submitObserveConfigDemandState, timeout = 5.seconds)
          .check(submitObserveConfigCurrentState) shouldBe true
      }
    }
  }

  // DEOPSCSW-198: Enable Assembly to accept a Setup or an Observe configuration
  // DEOPSCSW-200: Send Submit Configuration command
  // DEOPSCSW-203: Write component-specific verification code
  // DEOPSCSW-204: Sender to know that Submit configuration command's validation was successful
  // DEOPSCSW-213: Sender to know that oneway configuration command's validation was successful
  // DEOPSCSW-293: Sanitise handlers in Component Handlers
  // DEOPSCSW-306: Include runId in Command response
  test("onOneway hook should be invoked and command validation should be successful on receiving Observe config") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        val supervisorRef                                              = createSupervisorAndStartTLA(info, mocks)
        val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]

        supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

        val obsId: ObsId          = ObsId("Obs001")
        val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
        val setup: Setup          = Setup(prefix, CommandName("move.success"), Some(obsId), Set(param))

        supervisorRef ! Oneway(setup, commandValidationResponseProbe.ref)

        // verify that validateOneway handler is invoked
        val onewaySetupValidationCurrentState = compStateProbe.expectMessageType[CurrentState]
        val onewaySetupValidationDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice)))
        DemandMatcher(onewaySetupValidationDemandState, timeout = 5.seconds)
          .check(onewaySetupValidationCurrentState) shouldBe true
        commandValidationResponseProbe.expectMessage(Accepted(setup.runId))

        // verify that onSetup handler is invoked and that data is transferred
        val onewaySetupCommandCurrentState = compStateProbe.expectMessageType[CurrentState]
        val onewaySetupCommandDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(oneWayCommandChoice)))
        DemandMatcher(onewaySetupCommandDemandState, timeout = 5.seconds)
          .check(onewaySetupCommandCurrentState) shouldBe true

        // verify that OneWay command is received by handler
        val onewaySetupConfigCurrentState = compStateProbe.expectMessageType[CurrentState]
        val onewaySetupConfigDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(setupConfigChoice), param))
        DemandMatcher(onewaySetupConfigDemandState, timeout = 5.seconds)
          .check(onewaySetupConfigCurrentState) shouldBe true

        val observe: Observe = Observe(prefix, CommandName("move.success"), Some(obsId), Set(param))

        supervisorRef ! Oneway(observe, commandValidationResponseProbe.ref)

        // verify that validateOneway handler is invoked
        val oneWayObserveValidationCurrentState = compStateProbe.expectMessageType[CurrentState]
        val oneWayObserveValidationDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(commandValidationChoice)))
        DemandMatcher(oneWayObserveValidationDemandState, timeout = 5.seconds)
          .check(oneWayObserveValidationCurrentState) shouldBe true
        commandValidationResponseProbe.expectMessage(Accepted(observe.runId))

        // verify that onObserve handler is invoked and parameter is successfully transferred
        val oneWayObserveCommandCurrentState = compStateProbe.expectMessageType[CurrentState]
        val oneWayObserveCommandDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(oneWayCommandChoice)))
        DemandMatcher(oneWayObserveCommandDemandState, timeout = 5.seconds)
          .check(oneWayObserveCommandCurrentState) shouldBe true

        // verify that OneWay command is received by handler
        val oneWayObserveConfigCurrentState = compStateProbe.expectMessageType[CurrentState]
        val oneWayObserveConfigDemandState =
          DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(observeConfigChoice), param))
        DemandMatcher(oneWayObserveConfigDemandState, timeout = 5.seconds)
          .check(oneWayObserveConfigCurrentState) shouldBe true
      }
    }
  }

  // DEOPSCSW-206: Sender to know that Submit configuration command's validation failed
  // DEOPSCSW-214: Sender to know that oneway configuration command's validation failed
  test("component handler should be able to validate a Setup or Observe command as failure during validation") {
    forAll(testData) { (info: ComponentInfo) ⇒
      {
        val mocks                                                      = frameworkTestMocks()
        val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]
        import mocks._
        val supervisorRef = createSupervisorAndStartTLA(hcdInfo, mocks)

        supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

        val obsId: ObsId          = ObsId("Obs001")
        val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
        // setup to receive Success in validation result
        val setup: Setup = Setup(prefix, CommandName("move.failure"), Some(obsId), Set(param))

        supervisorRef ! Submit(setup, commandValidationResponseProbe.ref)
        commandValidationResponseProbe.expectMessageType[Invalid]

        supervisorRef ! Oneway(setup, commandValidationResponseProbe.ref)
        commandValidationResponseProbe.expectMessageType[Invalid]
      }
    }
  }

  test("onGoOffline and goOnline hooks of comp handlers should be invoked when supervisor receives Lifecycle messages") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        val supervisorRef = createSupervisorAndStartTLA(info, mocks)

        supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

        supervisorRef ! Lifecycle(GoOffline)

        val offlineCurrentState = compStateProbe.expectMessageType[CurrentState]
        val offlineDemandState  = DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(offlineChoice)))
        DemandMatcher(offlineDemandState, timeout = 5.seconds).check(offlineCurrentState) shouldBe true

        supervisorRef ! Lifecycle(GoOnline)

        val onlineCurrentState = compStateProbe.expectMessageType[CurrentState]
        val onlineDemandState  = DemandState(prefix, StateName("testStateName"), Set(choiceKey.set(onlineChoice)))
        DemandMatcher(onlineDemandState, timeout = 5.seconds).check(onlineCurrentState) shouldBe true
      }
    }
  }

  test("should invoke onShutdown hook when supervisor restarts component using Restart external message") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        val containerIdleMessageProbe = TestProbe[ContainerIdleMessage]
        val supervisorRef             = createSupervisorAndStartTLA(info, mocks, containerIdleMessageProbe.ref)

        supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        containerIdleMessageProbe.expectMessage(SupervisorLifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

        supervisorRef ! Restart

        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))
        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))

        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        containerIdleMessageProbe.expectMessage(SupervisorLifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

        verify(locationService).unregister(any[AkkaConnection])
        verify(locationService, times(2)).register(akkaRegistration)
      }
    }
  }

  test("running component should ignore RunOnline lifecycle message when it is already online") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        val supervisorRef = createSupervisorAndStartTLA(info, mocks)

        supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

        supervisorRef ! Lifecycle(GoOnline)
        compStateProbe.expectNoMessage(1.seconds)
      }
    }
  }

  test("running component should ignore RunOffline lifecycle message when it is already offline") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        val supervisorRef = createSupervisorAndStartTLA(info, mocks)

        supervisorRef ! ComponentStateSubscription(Subscribe(compStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

        compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
        lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

        supervisorRef ! Lifecycle(GoOffline)
        compStateProbe.expectMessageType[CurrentState]

        supervisorRef ! Lifecycle(GoOffline)
        compStateProbe.expectNoMessage(1.seconds)

        supervisorRef ! Lifecycle(GoOnline)
        compStateProbe.expectMessageType[CurrentState]
      }
    }
  }

  // DEOPSCSW-284: Move Timeouts to Config file
  test("handle InitializeTimeout by stopping TLA") {
    val supervisorLifecycleStateProbe: TestProbe[SupervisorLifecycleState] = TestProbe[SupervisorLifecycleState]

    val testData = Table(
      "componentInfo",
      hcdInfoWithInitializeTimeout,
      jHcdInfoWithInitializeTimeout
    )

    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        val componentStateProbe: TestProbe[CurrentState] = TestProbe[CurrentState]

        val supervisorRef = createSupervisorAndStartTLA(info, mocks)
        supervisorRef ! ComponentStateSubscription(Subscribe(componentStateProbe.ref))
        supervisorRef ! LifecycleStateSubscription(Subscribe(lifecycleStateProbe.ref))

        componentStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(shutdownChoice))))

        supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)
        supervisorLifecycleStateProbe.expectMessage(SupervisorLifecycleState.Idle)
        verify(locationService, never()).register(akkaRegistration)
      }
    }
  }
}
