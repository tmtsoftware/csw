package csw.common.framework.integration

import akka.typed.testkit.scaladsl.TestProbe
import akka.typed.{ActorRef, Behavior}
import csw.common.ccs.CommandStatus.CommandResponse
import csw.common.ccs.DemandMatcher
import csw.common.components.ComponentStatistics
import csw.common.framework.ComponentInfos._
import csw.common.framework.internal.supervisor.{SupervisorBehaviorFactory, SupervisorMode}
import csw.common.framework.models.CommandMessage.Oneway
import csw.common.framework.models.FromSupervisorMessage.SupervisorModeChanged
import csw.common.framework.models.PubSub.Publish
import csw.common.framework.models.RunningMessage.Lifecycle
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline, Restart}
import csw.common.framework.models.{ContainerIdleMessage, LifecycleStateChanged, SupervisorExternalMessage}
import csw.common.framework.{FrameworkTestSuite, TestMocks}
import csw.param.commands.{CommandInfo, Setup}
import csw.param.generics.{KeyType, Parameter}
import csw.param.states.{CurrentState, DemandState}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
// DEOPSCSW-165: CSW Assembly Creation
// DEOPSCSW-166: CSW HCD Creation
// DEOPSCSW-176: Provide Infrastructure to manage TMT lifecycle
// DEOPSCSW-177: Hooks for lifecycle management
class SupervisorIntegrationTest extends FrameworkTestSuite with BeforeAndAfterEach {
  import csw.common.components.SampleComponentState._

  var supervisorBehavior: Behavior[SupervisorExternalMessage]    = _
  var supervisorRef: ActorRef[SupervisorExternalMessage]         = _
  var containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = _

  private def createSupervisorAndStartTLA(testMocks: TestMocks): Unit = {
    import testMocks._
    containerIdleMessageProbe = TestProbe[ContainerIdleMessage]

    supervisorBehavior = SupervisorBehaviorFactory.make(
      Some(containerIdleMessageProbe.ref),
      hcdInfo,
      locationService,
      registrationFactory,
      pubSubBehaviorFactory
    )

    // it creates supervisor which in turn spawns components TLA and sends Initialize and Run message to TLA
    supervisorRef = Await.result(system.systemActorOf(supervisorBehavior, "comp-supervisor"), 5.seconds)
  }

  test("onInitialized and onRun hooks of comp handlers should be invoked when supervisor creates comp") {
    val testMockData = testMocks
    import testMockData._

    createSupervisorAndStartTLA(testMockData)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    containerIdleMessageProbe.expectMsg(SupervisorModeChanged(supervisorRef, SupervisorMode.Running))
  }

  // DEOPSCSW-179: Unique Action for a component
  test("onDomainMsg hook of comp handlers should be invoked when supervisor receives Domain message") {
    val testMockData = testMocks
    import testMockData._

    createSupervisorAndStartTLA(testMockData)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    supervisorRef ! ComponentStatistics(1)

    val domainCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val domainDemandState  = DemandState(prefix, Set(choiceKey.set(domainChoice)))
    DemandMatcher(domainDemandState).check(domainCurrentState.data) shouldBe true
  }

  test("onControlCommand hook of comp handlers should be invoked when supervisor receives Control command") {
    val testMockData = testMocks
    import testMockData._
    createSupervisorAndStartTLA(testMockData)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    val commandInfo: CommandInfo = "Obs001"
    val param: Parameter[Int]    = KeyType.IntKey.make("encoder").set(22)
    val setup: Setup             = Setup(commandInfo, prefix, Set(param))

    supervisorRef ! Oneway(setup, TestProbe[CommandResponse].ref)

    val commandCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val commandDemandState  = DemandState(prefix, Set(choiceKey.set(commandChoice)))
    DemandMatcher(commandDemandState).check(commandCurrentState.data) shouldBe true
  }

  test("onGoOffline and goOnline hooks of comp handlers should be invoked when supervisor receives Lifecycle messages") {
    val testMockData = testMocks
    import testMockData._
    createSupervisorAndStartTLA(testMockData)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    supervisorRef ! Lifecycle(GoOffline)

    val offlineCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val offlineDemandState  = DemandState(prefix, Set(choiceKey.set(offlineChoice)))
    DemandMatcher(offlineDemandState).check(offlineCurrentState.data) shouldBe true

    supervisorRef ! Lifecycle(GoOnline)

    val onlineCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val onlineDemandState  = DemandState(prefix, Set(choiceKey.set(onlineChoice)))
    DemandMatcher(onlineDemandState).check(onlineCurrentState.data) shouldBe true
  }

  test(
    "running component should throw TriggerRestartException on Restart lifecycle message using which supervisor uses supervision strategy to restart it"
  ) {
    val testMockData = testMocks
    import testMockData._

    createSupervisorAndStartTLA(testMockData)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))
    containerIdleMessageProbe.expectMsg(SupervisorModeChanged(supervisorRef, SupervisorMode.Running))

    supervisorRef ! Lifecycle(Restart)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(shutdownChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    verify(locationService, times(2)).register(akkaRegistration)
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))

    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))
    containerIdleMessageProbe.expectMsg(SupervisorModeChanged(supervisorRef, SupervisorMode.Running))
  }

  test("running component should ignore RunOnline lifecycle message when it is already online") {
    val testMockData = testMocks
    import testMockData._
    createSupervisorAndStartTLA(testMockData)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    supervisorRef ! Lifecycle(GoOnline)
    compStateProbe.expectNoMsg(1.seconds)
  }

  test("running component should ignore RunOffline lifecycle message when it is already offline") {
    val testMockData = testMocks
    import testMockData._
    createSupervisorAndStartTLA(testMockData)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    supervisorRef ! Lifecycle(GoOffline)
    compStateProbe.expectMsgType[Publish[CurrentState]]

    supervisorRef ! Lifecycle(GoOffline)
    compStateProbe.expectNoMsg(1.seconds)

    supervisorRef ! Lifecycle(GoOnline)
    compStateProbe.expectMsgType[Publish[CurrentState]]
  }

  ignore("onShutdown hook of comp handlers should be invoked when supervisor receives Shutdown message") {
    val testMockData = testMocks
    import testMockData._
    createSupervisorAndStartTLA(testMockData)

    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(runChoice)))))
    lifecycleStateProbe.expectMsg(Publish(LifecycleStateChanged(supervisorRef, SupervisorMode.Running)))

    untypedSystem.terminate()

    val shutdownCurrentState = compStateProbe.expectMsgType[Publish[CurrentState]]
    val shutdownDemandState  = DemandState(prefix, Set(choiceKey.set(shutdownChoice)))
    DemandMatcher(shutdownDemandState).check(shutdownCurrentState.data) shouldBe true

  }
}
