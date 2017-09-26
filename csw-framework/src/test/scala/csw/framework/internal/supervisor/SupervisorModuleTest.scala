package csw.framework.internal.supervisor

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.ccs.Invalid
import csw.ccs.{Accepted, CommandResponse, DemandMatcher}
import csw.common.components.ComponentStatistics
import csw.framework.ComponentInfos._
import csw.framework.javadsl.commons.JComponentInfos.{jHcdInfo, jHcdInfoWithInitializeTimeout, jHcdInfoWithRunTimeout}
import csw.framework.javadsl.components.JComponentDomainMessage
import csw.framework.models.CommandMessage.{Oneway, Submit}
import csw.framework.models.FromSupervisorMessage.SupervisorLifecycleStateChanged
import csw.framework.models.PubSub.Publish
import csw.framework.models.RunningMessage.{DomainMessage, Lifecycle}
import csw.framework.models.SupervisorCommonMessage.GetSupervisorLifecycleState
import csw.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.framework.models._
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.param.commands.{CommandInfo, Setup}
import csw.param.generics.{KeyType, Parameter}
import csw.param.states.{CurrentState, DemandState}
import csw.services.location.models.Connection.AkkaConnection
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
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

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
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        supervisorRef ! domainMessage

        val domainCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val domainDemandState  = DemandState(prefix, Set(choiceKey.set(domainChoice)))
        DemandMatcher(domainDemandState).check(domainCurrentState.data) shouldBe true
      }
    }
  }

  test("onControlCommand hook of comp handlers should be invoked when supervisor receives Control command") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)
        val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

        val commandInfo: CommandInfo = "Obs001"
        val param: Parameter[Int]    = KeyType.IntKey.make("encoder").set(22)
        val setup: Setup             = Setup(commandInfo, prefix, Set(param))

        supervisorRef ! Submit(setup, commandValidationResponseProbe.ref)
        val submitCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val submitCommandDemandState  = DemandState(prefix, Set(choiceKey.set(submitCommandChoice)))
        DemandMatcher(submitCommandDemandState).check(submitCommandCurrentState.data) shouldBe true

        supervisorRef ! Oneway(setup, commandValidationResponseProbe.ref)
        val onewayCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
        val onewayCommandDemandState  = DemandState(prefix, Set(choiceKey.set(oneWayCommandChoice)))
        DemandMatcher(onewayCommandDemandState).check(onewayCommandCurrentState.data) shouldBe true
      }
    }
  }

  test("component handler should be able to validate a Setup or Observe command as successful validation") {
    val mocks                                                      = frameworkTestMocks()
    val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]

    import mocks._
    createSupervisorAndStartTLA(hcdInfo, mocks)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    val commandInfo: CommandInfo = "Obs001"
    val param: Parameter[Int]    = KeyType.IntKey.make("encoder").set(22)
    // setup to receive Success in validation result
    val setup: Setup = Setup(commandInfo, successPrefix, Set(param))

    supervisorRef ! Submit(setup, commandValidationResponseProbe.ref)
    val submitCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val submitCommandDemandState  = DemandState(prefix, Set(choiceKey.set(submitCommandChoice)))
    DemandMatcher(submitCommandDemandState).check(submitCommandCurrentState.data) shouldBe true
    commandValidationResponseProbe.expectMsg(Accepted)

    supervisorRef ! Oneway(setup, commandValidationResponseProbe.ref)
    val onewayCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val onewayCommandDemandState  = DemandState(prefix, Set(choiceKey.set(oneWayCommandChoice)))
    DemandMatcher(onewayCommandDemandState).check(onewayCommandCurrentState.data) shouldBe true
    commandValidationResponseProbe.expectMsg(Accepted)
  }

  test("component handler should be able to validate a Setup or Observe command as failure during validation") {
    val mocks                                                      = frameworkTestMocks()
    val commandValidationResponseProbe: TestProbe[CommandResponse] = TestProbe[CommandResponse]
    import mocks._
    createSupervisorAndStartTLA(hcdInfo, mocks)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    val commandInfo: CommandInfo = "Obs001"
    val param: Parameter[Int]    = KeyType.IntKey.make("encoder").set(22)
    // setup to receive Success in validation result
    val setup: Setup = Setup(commandInfo, failedPrefix, Set(param))

    supervisorRef ! Submit(setup, commandValidationResponseProbe.ref)
    val submitCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val submitCommandDemandState  = DemandState(prefix, Set(choiceKey.set(submitCommandChoice)))
    DemandMatcher(submitCommandDemandState).check(submitCommandCurrentState.data) shouldBe true
    commandValidationResponseProbe.expectMsgType[Invalid]

    supervisorRef ! Oneway(setup, commandValidationResponseProbe.ref)
    val onewayCommandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val onewayCommandDemandState  = DemandState(prefix, Set(choiceKey.set(oneWayCommandChoice)))
    DemandMatcher(onewayCommandDemandState).check(onewayCommandCurrentState.data) shouldBe true
    commandValidationResponseProbe.expectMsgType[Invalid]
  }

  test("onGoOffline and goOnline hooks of comp handlers should be invoked when supervisor receives Lifecycle messages") {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
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

  test(
    "running component should invoke onShutdown hook when supervisor restarts component using Restart external message"
  ) {
    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
        lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))
        containerIdleMessageProbe.expectMsg(
          SupervisorLifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)
        )

        supervisorRef ! Restart

        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

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
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
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
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
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

  // DEOPSCSW-284: Move Timeouts to Config file
  test("handle RunTimeout by stopping TLA") {

    val testData = Table(
      "componentInfo",
      hcdInfoWithRunTimeout,
      jHcdInfoWithRunTimeout
    )

    forAll(testData) { (info: ComponentInfo) =>
      {
        val mocks = frameworkTestMocks()
        import mocks._
        createSupervisorAndStartTLA(info, mocks)
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
        compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))

        supervisorRef ! GetSupervisorLifecycleState(supervisorLifecycleStateProbe.ref)
        supervisorLifecycleStateProbe.expectMsg(SupervisorLifecycleState.Idle)
        verify(locationService).register(akkaRegistration)
      }
    }
  }
}
