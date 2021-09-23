package csw.framework.internal.component

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.Behavior
import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.command.client.messages.FromComponentLifecycleMessage.Running
import csw.command.client.messages.TopLevelActorIdleMessage.Initialize
import csw.command.client.messages.{FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.command.client.{CommandResponseManager, MiniCRM}
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{ComponentInfos, CurrentStatePublisher, FrameworkTestSuite}
import csw.time.core.models.UTCTime
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers

// DEOPSCSW-165-CSW Assembly Creation
// DEOPSCSW-166-CSW HCD Creation
class ComponentBehaviorTest extends FrameworkTestSuite with MockitoSugar with Matchers {
  class TestData(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {

    val sampleComponentHandler: ComponentHandlers = mock[ComponentHandlers]

    val commandResponseManager: CommandResponseManager = mock[CommandResponseManager]
    when(commandResponseManager.commandResponseManagerActor).thenReturn(TestProbe[MiniCRM.CRMMessage]().ref)

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

    val factory                             = new TestComponentBehaviorFactory(sampleComponentHandler)
    private val behavior: Behavior[Nothing] = factory.make(supervisorProbe.ref, cswCtx)
    val componentBehaviorTestKit: BehaviorTestKit[TopLevelActorMessage] =
      BehaviorTestKit(behavior.asInstanceOf[Behavior[TopLevelActorMessage]])
  }

  test("component should send itself initialize message and handle initialization | DEOPSCSW-165, DEOPSCSW-166") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]()
    val testData        = new TestData(supervisorProbe)
    import testData._

    componentBehaviorTestKit.selfInbox().receiveMessage() shouldBe Initialize

    componentBehaviorTestKit.run(Initialize)
    supervisorProbe.expectMessageType[Running]
    verify(sampleComponentHandler).initialize()
    verify(sampleComponentHandler).isOnline_=(true)
  }

  // CSW-37: Add diagnosticMode handler to component handlers
  test("component should handle DiagnosticMode message | DEOPSCSW-165, DEOPSCSW-166, CSW-37") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]()
    val testData        = new TestData(supervisorProbe)
    import testData._

    val startTime = UTCTime.now()
    val hint      = "engineering"
    componentBehaviorTestKit.run(Initialize)
    componentBehaviorTestKit.run(DiagnosticMode(startTime, hint))
    verify(sampleComponentHandler).onDiagnosticMode(startTime, hint)
  }
}
