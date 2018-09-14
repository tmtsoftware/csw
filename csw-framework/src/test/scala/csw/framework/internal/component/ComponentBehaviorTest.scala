package csw.framework.internal.component

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.Behavior
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{ComponentInfos, CurrentStatePublisher, FrameworkTestSuite}
import csw.command.messages.FromComponentLifecycleMessage.Running
import csw.command.messages.TopLevelActorIdleMessage.Initialize
import csw.location.api.scaladsl.LocationService
import csw.command.messages.{CommandResponseManagerMessage, FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.alarm.api.scaladsl.AlarmService
import csw.command.CommandResponseManager
import csw.event.api.scaladsl.EventService
import org.mockito.Mockito._
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.Future

// DEOPSCSW-165-CSW Assembly Creation
// DEOPSCSW-166-CSW HCD Creation
class ComponentBehaviorTest extends FrameworkTestSuite with MockitoSugar with Matchers {

  class TestData(supervisorProbe: TestProbe[FromComponentLifecycleMessage]) {

    val sampleComponentHandler: ComponentHandlers = mock[ComponentHandlers]
    when(sampleComponentHandler.initialize()).thenReturn(Future.unit)

    val commandResponseManager: CommandResponseManager = mock[CommandResponseManager]
    when(commandResponseManager.commandResponseManagerActor).thenReturn(TestProbe[CommandResponseManagerMessage].ref)
    val locationService: LocationService = mock[LocationService]
    val eventService: EventService       = mock[EventService]
    val alarmService: AlarmService       = mock[AlarmService]
    val cswCtx: CswContext = new CswContext(
      locationService,
      eventService,
      alarmService,
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

  test("component should send itself initialize message and handle initialization") {
    val supervisorProbe = TestProbe[FromComponentLifecycleMessage]
    val testData        = new TestData(supervisorProbe)
    import testData._

    componentBehaviorTestKit.selfInbox.receiveMessage() shouldBe Initialize

    componentBehaviorTestKit.run(Initialize)
    supervisorProbe.expectMessageType[Running]
    verify(sampleComponentHandler).initialize()
    verify(sampleComponentHandler).isOnline_=(true)
  }
}
