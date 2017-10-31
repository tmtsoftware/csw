package csw.framework.internal.supervisor

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.ccs.DemandMatcher
import csw.common.components.ComponentStatistics
import csw.framework.ComponentInfos._
import csw.framework.javadsl.commons.JComponentInfos.{jHcdInfo, jHcdInfoWithInitializeTimeout}
import csw.framework.javadsl.components.JComponentDomainMessage
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.CommandValidationResponse.{Accepted, Invalid}
import csw.messages.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.messages.PubSub.Publish
import csw.messages.RunningMessage.{DomainMessage, Lifecycle}
import csw.messages.SupervisorCommonMessage.GetSupervisorLifecycleState
import csw.messages.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.messages._
import csw.messages.ccs.commands.{Observe, Setup}
import csw.messages.framework.{ComponentInfo, SupervisorLifecycleState}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.ObsId
import csw.messages.params.states.{CurrentState, DemandState}
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
  import csw.common.components.SampleComponentState._

  val supervisorLifecycleStateProbe: TestProbe[SupervisorLifecycleState] = TestProbe[SupervisorLifecycleState]
  var supervisorBehavior: Behavior[SupervisorExternalMessage]            = _
  var supervisorRef: ActorRef[SupervisorExternalMessage]                 = _
  var containerIdleMessageProbe: TestProbe[ContainerIdleMessage]         = _

  val testData = Table(
    "componentInfo",
    hcdInfo,
    jHcdInfo,
  )

  private def createSupervisorAndStartTLA(componentInfo: ComponentInfo, testMocks: FrameworkTestMocks): Unit = {
    import testMocks._
    containerIdleMessageProbe = TestProbe[ContainerIdleMessage]

    supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(containerIdleMessageProbe.ref),
      componentInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = untypedSystem.spawnAnonymous(supervisorBehavior)
  }

  test("onInitialized and onRun hooks of comp handlers should be invoked when supervisor creates comp") {

    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))
        containerIdleMessageProbe.expectMsg(
          SupervisorLifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)
        )
        verify(locationService).register(akkaRegistration)
      }
    }
  }

  // DEOPSCSW-179: Unique Action for a component
  test("onDomainMsg hook of comp handlers should be invoked when supervisor receives Domain message") {
    val testData = Table(
      ("componentInfo", "domainMessage"),
      (hcdInfo, ComponentStatistics(1)),
      (jHcdInfo, new JComponentDomainMessage())
    )

    forAll(testData) { (info: ComponentInfo, domainMessage: DomainMessage) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        supervisorRef ! domainMessage

        val domainCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val domainDemandState  = DemandState(prefix, Set(choiceKey.set(domainChoice)))
        DemandMatcher(domainDemandState).check(domainCurrentState.data) shouldBe true
      }
    }
  }

  // DEOPSCSW-198: Enable Assembly to accept a Setup or an Observe configuration
  // DEOPSCSW-199: Enable HCD to accept a Setup configuration
  // DEOPSCSW-200: Send Submit Configuration command
  // DEOPSCSW-204: Sender to know that Submit configuration command's validation was successful
  // DEOPSCSW-213: Sender to know that oneway configuration command's validation was successful
  // DEOPSCSW-293: Sanitise handlers in Component Handlers
  // DEOPSCSW-306: Include runId in validation response
  test("onSubmit hook should be invoked and command validation should be successful on receiving Setup config") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)
        val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        val obsId: ObsId          = ObsId("Obs001")
        val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
        val setup: Setup          = Setup(obsId, successPrefix, Set(param))

        supervisorRef ! Submit(setup, commandValidationResponseProbe.ref)
        // verify that onSubmit handler is invoked
        val submitSetupCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitSetupCommandDemandState  = DemandState(prefix, Set(choiceKey.set(submitCommandChoice)))
        DemandMatcher(submitSetupCommandDemandState).check(submitSetupCommandCurrentState.data) shouldBe true

        // verify that setup config is received by handler and provide check that data is transferred
        val submitSetupConfigCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitSetupConfigDemandState  = DemandState(prefix, Set(choiceKey.set(setupConfigChoice), param))
        DemandMatcher(submitSetupConfigDemandState).check(submitSetupConfigCurrentState.data) shouldBe true
        commandValidationResponseProbe.expectMsg(Accepted(setup.runId))

        val observe: Observe = Observe(obsId, successPrefix, Set(param))

        supervisorRef ! Submit(observe, commandValidationResponseProbe.ref)

        // verify that onSubmit handler is invoked
        val submitCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitCommandDemandState  = DemandState(prefix, Set(choiceKey.set(submitCommandChoice)))
        DemandMatcher(submitCommandDemandState).check(submitCommandCurrentState.data) shouldBe true
        commandValidationResponseProbe.expectMsg(Accepted(observe.runId))

        // verify that observe config is received by handler and provide check that data is transferred
        val submitObserveConfigCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitObserveConfigDemandState  = DemandState(prefix, Set(choiceKey.set(observeConfigChoice), param))
        DemandMatcher(submitObserveConfigDemandState).check(submitObserveConfigCurrentState.data) shouldBe true
      }
    }
  }

  // DEOPSCSW-198: Enable Assembly to accept a Setup or an Observe configuration
  // DEOPSCSW-200: Send Submit Configuration command
  // DEOPSCSW-204: Sender to know that Submit configuration command's validation was successful
  // DEOPSCSW-213: Sender to know that oneway configuration command's validation was successful
  // DEOPSCSW-293: Sanitise handlers in Component Handlers
  // DEOPSCSW-306: Include runId in validation response
  test("onOneway hook should be invoked and command validation should be successful on receiving Observe config") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(hcdInfo, mocks)
        val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        val obsId: ObsId          = ObsId("Obs001")
        val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
        val setup: Setup          = Setup(obsId, successPrefix, Set(param))

        supervisorRef ! Oneway(setup, commandValidationResponseProbe.ref)
        // verify that onSetup handler is invoked and that data is transferred
        val onewaySetupCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val onewaySetupCommandDemandState  = DemandState(prefix, Set(choiceKey.set(oneWayCommandChoice)))
        DemandMatcher(onewaySetupCommandDemandState).check(onewaySetupCommandCurrentState.data) shouldBe true

        // verify that OneWay command is received by handler
        val onewaySetupConfigCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val onewaySetupConfigDemandState  = DemandState(prefix, Set(choiceKey.set(setupConfigChoice), param))
        DemandMatcher(onewaySetupConfigDemandState).check(onewaySetupConfigCurrentState.data) shouldBe true
        commandValidationResponseProbe.expectMsg(Accepted(setup.runId))

        val observe: Observe = Observe(obsId, successPrefix, Set(param))

        supervisorRef ! Oneway(observe, commandValidationResponseProbe.ref)
        // verify that onObserve handler is invoked and parameter is successfully transferred
        val oneWayObserveCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val oneWayObserveCommandDemandState  = DemandState(prefix, Set(choiceKey.set(oneWayCommandChoice)))
        DemandMatcher(oneWayObserveCommandDemandState).check(oneWayObserveCommandCurrentState.data) shouldBe true

        // verify that OneWay command is received by handler
        val oneWayObserveConfigCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val oneWayObserveConfigDemandState  = DemandState(prefix, Set(choiceKey.set(observeConfigChoice), param))
        DemandMatcher(oneWayObserveConfigDemandState).check(oneWayObserveConfigCurrentState.data) shouldBe true
        commandValidationResponseProbe.expectMsg(Accepted(observe.runId))
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
        createSupervisorAndStartTLA(hcdInfo, mocks)

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        val obsId: ObsId          = ObsId("Obs001")
        val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
        // setup to receive Success in validation result
        val setup: Setup = Setup(obsId, failedPrefix, Set(param))

        supervisorRef ! Submit(setup, commandValidationResponseProbe.ref)
        commandValidationResponseProbe.expectMsgType[Invalid]

        supervisorRef ! Oneway(setup, commandValidationResponseProbe.ref)
        commandValidationResponseProbe.expectMsgType[Invalid]
      }
    }
  }

  test("onGoOffline and goOnline hooks of comp handlers should be invoked when supervisor receives Lifecycle messages") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        supervisorRef ! Lifecycle(GoOffline)

        val offlineCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val offlineDemandState  = DemandState(prefix, Set(choiceKey.set(offlineChoice)))
        DemandMatcher(offlineDemandState).check(offlineCurrentState.data) shouldBe true

        supervisorRef ! Lifecycle(GoOnline)

        val onlineCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val onlineDemandState  = DemandState(prefix, Set(choiceKey.set(onlineChoice)))
        DemandMatcher(onlineDemandState).check(onlineCurrentState.data) shouldBe true
      }
    }
  }

  test("should invoke onShutdown hook when supervisor restarts component using Restart external message") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))
        containerIdleMessageProbe.expectMsg(
          SupervisorLifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)
        )

        supervisorRef ! Restart

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))
        containerIdleMessageProbe.expectMsg(
          SupervisorLifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)
        )

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
        createSupervisorAndStartTLA(info, mocks)

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        supervisorRef ! Lifecycle(GoOnline)
        compStateProbe.expectNoMsg(1.seconds)
      }
    }
  }

  test("running component should ignore RunOffline lifecycle message when it is already offline") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        supervisorRef ! Lifecycle(GoOffline)
        compStateProbe.expectMsgType[Publish[CurrentState]]

        supervisorRef ! Lifecycle(GoOffline)
        compStateProbe.expectNoMsg(1.seconds)

        supervisorRef ! Lifecycle(GoOnline)
        compStateProbe.expectMsgType[Publish[CurrentState]]
      }
    }
  }

  // DEOPSCSW-284: Move Timeouts to Config file
  test("handle InitializeTimeout by stopping TLA") {

    val testData = Table(
      "componentInfo",
      hcdInfoWithInitializeTimeout,
      jHcdInfoWithInitializeTimeout
    )

    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

        supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)
        supervisorLifecycleStateProbe.expectMsg(SupervisorLifecycleState.Idle)
        verify(locationService, never()).register(akkaRegistration)
      }
    }
  }
}
