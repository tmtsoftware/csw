package csw.framework.internal.component

import akka.actor.typed.Behavior
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestProbe}
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.framework.{ComponentInfos, FrameworkTestSuite}
import csw.messages.scaladsl.FromComponentLifecycleMessage.Running
import csw.messages.scaladsl.TopLevelActorIdleMessage.Initialize
import csw.messages.scaladsl.{CommandResponseManagerMessage, FromComponentLifecycleMessage, TopLevelActorMessage}
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.event.scaladsl.EventService
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

    val factory = new TestComponentBehaviorFactory(sampleComponentHandler)

    private val behavior: Behavior[Nothing] = factory.make(
      ComponentInfos.hcdInfo,
      supervisorProbe.ref,
      mock[CurrentStatePublisher],
      commandResponseManager,
      locationService,
      eventService,
      frameworkTestMocks().loggerFactory
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
