package csw.framework.internal.supervisor

import java.util.UUID

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.ccs.internal.matchers.DemandMatcher
import csw.common.components.ComponentStatistics
import csw.framework.ComponentInfos._
import csw.framework.javadsl.commons.JComponentInfos.{jHcdInfo, jHcdInfoWithInitializeTimeout}
import csw.framework.javadsl.components.JComponentDomainMessage
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.messages.CommandMessage.{Oneway, Submit}
import csw.messages.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.messages.RunningMessage.{DomainMessage, Lifecycle}
import csw.messages.SupervisorCommonMessage.GetSupervisorLifecycleState
import csw.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.messages.ccs.commands.CommandResponse.{Accepted, Invalid}
import csw.messages.ccs.commands.{CommandResponse, Observe, Setup}
import csw.messages.framework.{ComponentInfo, SupervisorLifecycleState}
import csw.messages.location.Connection.AkkaConnection
import csw.messages.models.LockingResponse._
import csw.messages.models.PubSub.Publish
import csw.messages.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.messages.models.{LifecycleStateChanged, LockingResponse}
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.{ObsId, Prefix}
import csw.messages.params.states.{CurrentState, DemandState}
import csw.messages.{models, _}
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
        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )

        supervisorRef ! domainMessage

        val domainCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val domainDemandState  = DemandState(prefix, Set(choiceKey.set(domainChoice)))
        DemandMatcher(domainDemandState, timeout = 5.seconds).check(domainCurrentState.data) shouldBe true
      }
    }
  }

  // DEOPSCSW-198: Enable Assembly to accept a Setup or an Observe configuration
  // DEOPSCSW-199: Enable HCD to accept a Setup configuration
  // DEOPSCSW-200: Send Submit Configuration command
  // DEOPSCSW-203: Write component-specific verification code
  // DEOPSCSW-204: Sender to know that Submit configuration command's validation was successful
  // DEOPSCSW-213: Sender to know that oneway configuration command's validation was successful
  // DEOPSCSW-293: Sanitise handlers in Component Handlers
  // DEOPSCSW-306: Include runId in Command response
  test("onSubmit hook should be invoked and command validation should be successful on receiving Setup config") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)
        val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )

        val obsId: ObsId          = ObsId("Obs001")
        val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
        val setup: Setup          = Setup(obsId, successPrefix, Set(param))

        supervisorRef ! Submit(setup, commandValidationResponseProbe.ref)

        // verify that validateSubmit handler is invoked
        val submitSetupValidationCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitSetupValidationDemandState  = DemandState(prefix, Set(choiceKey.set(commandValidationChoice)))
        DemandMatcher(submitSetupValidationDemandState, timeout = 5.seconds)
          .check(submitSetupValidationCurrentState.data) shouldBe true
        commandValidationResponseProbe.expectMsg(Accepted(setup.runId))

        // verify that onSubmit handler is invoked
        val submitSetupCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitSetupCommandDemandState  = DemandState(prefix, Set(choiceKey.set(submitCommandChoice)))
        DemandMatcher(submitSetupCommandDemandState, timeout = 5.seconds)
          .check(submitSetupCommandCurrentState.data) shouldBe true

        // verify that setup config is received by handler and provide check that data is transferred
        val submitSetupConfigCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitSetupConfigDemandState  = DemandState(prefix, Set(choiceKey.set(setupConfigChoice), param))
        DemandMatcher(submitSetupConfigDemandState, timeout = 5.seconds)
          .check(submitSetupConfigCurrentState.data) shouldBe true

        val observe: Observe = Observe(obsId, successPrefix, Set(param))

        supervisorRef ! Submit(observe, commandValidationResponseProbe.ref)

        // verify that validateSubmit handler is invoked
        val submitValidationCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitValidationDemandState  = DemandState(prefix, Set(choiceKey.set(commandValidationChoice)))
        DemandMatcher(submitValidationDemandState, timeout = 5.seconds)
          .check(submitValidationCurrentState.data) shouldBe true
        commandValidationResponseProbe.expectMsg(Accepted(observe.runId))

        // verify that onSubmit handler is invoked
        val submitCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitCommandDemandState  = DemandState(prefix, Set(choiceKey.set(submitCommandChoice)))
        DemandMatcher(submitCommandDemandState, timeout = 5.seconds).check(submitCommandCurrentState.data) shouldBe true

        // verify that observe config is received by handler and provide check that data is transferred
        val submitObserveConfigCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitObserveConfigDemandState  = DemandState(prefix, Set(choiceKey.set(observeConfigChoice), param))
        DemandMatcher(submitObserveConfigDemandState, timeout = 5.seconds)
          .check(submitObserveConfigCurrentState.data) shouldBe true
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
        createSupervisorAndStartTLA(info, mocks)
        val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )

        val obsId: ObsId          = ObsId("Obs001")
        val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(22)
        val setup: Setup          = Setup(obsId, successPrefix, Set(param))

        supervisorRef ! Oneway(setup, commandValidationResponseProbe.ref)

        // verify that validateOneway handler is invoked
        val onewaySetupValidationCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val onewaySetupValidationDemandState  = DemandState(prefix, Set(choiceKey.set(commandValidationChoice)))
        DemandMatcher(onewaySetupValidationDemandState, timeout = 5.seconds)
          .check(onewaySetupValidationCurrentState.data) shouldBe true
        commandValidationResponseProbe.expectMsg(Accepted(setup.runId))

        // verify that onSetup handler is invoked and that data is transferred
        val onewaySetupCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val onewaySetupCommandDemandState  = DemandState(prefix, Set(choiceKey.set(oneWayCommandChoice)))
        DemandMatcher(onewaySetupCommandDemandState, timeout = 5.seconds)
          .check(onewaySetupCommandCurrentState.data) shouldBe true

        // verify that OneWay command is received by handler
        val onewaySetupConfigCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val onewaySetupConfigDemandState  = DemandState(prefix, Set(choiceKey.set(setupConfigChoice), param))
        DemandMatcher(onewaySetupConfigDemandState, timeout = 5.seconds)
          .check(onewaySetupConfigCurrentState.data) shouldBe true

        val observe: Observe = Observe(obsId, successPrefix, Set(param))

        supervisorRef ! Oneway(observe, commandValidationResponseProbe.ref)

        // verify that validateOneway handler is invoked
        val oneWayObserveValidationCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val oneWayObserveValidationDemandState  = DemandState(prefix, Set(choiceKey.set(commandValidationChoice)))
        DemandMatcher(oneWayObserveValidationDemandState, timeout = 5.seconds)
          .check(oneWayObserveValidationCurrentState.data) shouldBe true
        commandValidationResponseProbe.expectMsg(Accepted(observe.runId))

        // verify that onObserve handler is invoked and parameter is successfully transferred
        val oneWayObserveCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val oneWayObserveCommandDemandState  = DemandState(prefix, Set(choiceKey.set(oneWayCommandChoice)))
        DemandMatcher(oneWayObserveCommandDemandState, timeout = 5.seconds)
          .check(oneWayObserveCommandCurrentState.data) shouldBe true

        // verify that OneWay command is received by handler
        val oneWayObserveConfigCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val oneWayObserveConfigDemandState  = DemandState(prefix, Set(choiceKey.set(observeConfigChoice), param))
        DemandMatcher(oneWayObserveConfigDemandState, timeout = 5.seconds)
          .check(oneWayObserveConfigCurrentState.data) shouldBe true
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
        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )

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
        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )

        supervisorRef ! Lifecycle(GoOffline)

        val offlineCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val offlineDemandState  = DemandState(prefix, Set(choiceKey.set(offlineChoice)))
        DemandMatcher(offlineDemandState, timeout = 5.seconds).check(offlineCurrentState.data) shouldBe true

        supervisorRef ! Lifecycle(GoOnline)

        val onlineCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val onlineDemandState  = DemandState(prefix, Set(choiceKey.set(onlineChoice)))
        DemandMatcher(onlineDemandState, timeout = 5.seconds).check(onlineCurrentState.data) shouldBe true
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
        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )
        containerIdleMessageProbe.expectMsg(
          SupervisorLifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)
        )

        supervisorRef ! Restart

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))

        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )
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
        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )

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
        lifecycleStateProbe.expectMsg(
          Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
        )

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

  //DEOPSCSW-222: Locking a component for a specific duration
  test("should able to lock and unlock a component") {
    val lockingStateProbe = TestProbe[LockingResponse]
    val mocks             = frameworkTestMocks()
    import mocks._

    createSupervisorAndStartTLA(assemblyInfo, mocks)

    // Assure that component is in running state
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    lifecycleStateProbe.expectMsg(
      Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
    )

    val token1 = UUID.randomUUID().toString
    val token2 = UUID.randomUUID().toString
    // Client 1 will lock an assembly
    supervisorRef ! Lock("wfos.prog.cloudcover.Client1", token1, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAcquired)

    // Client 2 tries to lock the assembly while Client 1 already has the lock
    supervisorRef ! Lock("wfos.prog.cloudcover.Client2", token2, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(
      ReAcquiringLockFailed(
        s"Invalid prefix [wfos.prog.cloudcover.Client2] or token [$token2] for re-acquiring the lock. Currently it is acquired by component: [wfos.prog.cloudcover.Client1]"
      )
    )

    // Client 1 re-acquires the lock by sending the same token again
    supervisorRef ! Lock("wfos.prog.cloudcover.Client1", token1, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAcquired)

    // Client 2 tries to unlock the assembly while Client 1 already has the lock
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client2", token2, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(
      ReleasingLockFailed(
        s"Invalid prefix [wfos.prog.cloudcover.Client2] or token [$token2] for releasing the lock. Currently it is acquired by component: [wfos.prog.cloudcover.Client1]"
      )
    )

    // Client 1 unlocks the assembly successfully
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client1", token1, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockReleased)

    // Client 1 tries to unlock the same assembly again while the lock is already released
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client1", token1, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAlreadyReleased)

    // Client 2 tries to unlock the same assembly while the lock is already released
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client2", token2, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAlreadyReleased)
  }

  // DEOPSCSW-222: Locking a component for a specific duration
  // DEOPSCSW-301: Support UnLocking
  test("should forward command messages from client that locked the component and reject for other clients ") {
    val lockingStateProbe    = TestProbe[LockingResponse]
    val commandResponseProbe = TestProbe[CommandResponse]

    val client1Prefix = Prefix("wfos.prog.cloudcover.Client1.success")
    val token1        = "token-1"
    val client2Prefix = Prefix("wfos.prog.cloudcover.Client2.success")
    val token2        = "token-2"
    val obsId         = ObsId("Obs001")

    val mocks = frameworkTestMocks()
    import mocks._

    createSupervisorAndStartTLA(assemblyInfo, mocks)

    // Assure that component is in running state
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    lifecycleStateProbe.expectMsg(
      Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))
    )

    // Client 1 will lock an assembly
    supervisorRef ! Lock(client1Prefix.toString, token1, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAcquired)

    // Client 1 sends submit command with tokenId in parameter set
    supervisorRef ! Submit(Setup.withLockToken(obsId, client1Prefix, token1), commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[Accepted]

    // Client 2 tries to send submit command while Client 1 has the lock
    supervisorRef ! Submit(Setup.withLockToken(obsId, client2Prefix, token2), commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[Invalid]

    // Client 1 unlocks the assembly
    supervisorRef ! Unlock(client1Prefix.toString, token1, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockReleased)

    // Client 2 tries to send submit command again after lock is released
    supervisorRef ! Submit(Setup.withLockToken(obsId, client2Prefix, token2), commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[Accepted]
  }
}
