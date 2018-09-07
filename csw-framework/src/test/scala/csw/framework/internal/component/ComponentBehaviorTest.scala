package csw.framework.internal.component

import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import akka.actor.typed.Behavior
import csw.framework.models.CswServices
import csw.framework.scaladsl.ComponentHandlers
import csw.framework.{ComponentInfos, CurrentStatePublisher, FrameworkTestSuite}
import csw.messages.FromComponentLifecycleMessage.Running
import csw.messages.TopLevelActorIdleMessage.Initialize
import csw.messages.{CommandResponseManagerMessage, FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.services.alarm.api.scaladsl.AlarmService
import csw.services.command.CommandResponseManager
import csw.services.event.api.scaladsl.EventService
import csw.services.location.scaladsl.LocationService
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
    val cswServices: CswServices = new CswServices(
      locationService,
      eventService,
      alarmService,
      frameworkTestMocks().loggerFactory,
      frameworkTestMocks().configClientService,
      mock[CurrentStatePublisher],
      commandResponseManager
    )

    val factory = new TestComponentBehaviorFactory(sampleComponentHandler)

    private val behavior: Behavior[Nothing] = factory.make(
      ComponentInfos.hcdInfo,
      supervisorProbe.ref,
      cswServices
    )
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
