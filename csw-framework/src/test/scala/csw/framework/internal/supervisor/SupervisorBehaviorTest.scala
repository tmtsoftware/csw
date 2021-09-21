package csw.framework.internal.supervisor

import akka.actor.testkit.typed.Effect
import akka.actor.testkit.typed.Effect.{Spawned, Watched}
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox, TestProbe}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.command.client.messages.FromComponentLifecycleMessage.Running
import csw.command.client.messages._
import csw.common.components.framework.SampleComponentBehaviorFactory
import csw.common.extensions.CswContextExtensions.RichCswContext
import csw.framework.ComponentInfos._
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.logging.models.Level.WARN
import csw.logging.models.LogMetadata
import csw.time.core.models.UTCTime
import org.mockito.{ArgumentMatchers, MockitoSugar}

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
class SupervisorBehaviorTest extends FrameworkTestSuite with MockitoSugar {
  val testMocks: FrameworkTestMocks = frameworkTestMocks()
  import testMocks._

  val containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = TestProbe[ContainerIdleMessage]()
  private val timerScheduler                                     = mock[TimerScheduler[SupervisorMessage]]

  doNothing
    .when(timerScheduler)
    .startSingleTimer(
      ArgumentMatchers.eq(SupervisorBehavior.InitializeTimerKey),
      ArgumentMatchers.any[SupervisorMessage],
      ArgumentMatchers.any[FiniteDuration]
    )

  doNothing.when(timerScheduler).cancel(ArgumentMatchers.eq(SupervisorBehavior.InitializeTimerKey))
  when(timerScheduler.isTimerActive(SupervisorBehavior.InitializeTimerKey)).thenReturn(true)

  val supervisorBehavior: Behavior[ComponentMessage] = createBehavior(timerScheduler)
  val componentTLAName                               = s"${hcdInfo.prefix}-${SupervisorBehavior.ComponentActorNameSuffix}"

  test("Supervisor should create child actors for TLA, pub-sub actor for lifecycle and component state | DEOPSCSW-163") {
    val supervisorBehaviorTestKit = BehaviorTestKit(supervisorBehavior)

    val effects: immutable.Seq[Effect] = supervisorBehaviorTestKit.retrieveAllEffects()

    val spawnedEffects = effects.map {
      case s: Spawned[_] => s.childName
      case _             => ""
    }

    (spawnedEffects should contain).allOf(
      componentTLAName,
      SupervisorBehavior.PubSubLifecycleActor
    )
  }

  test("Supervisor should watch child component actor [TLA] | DEOPSCSW-163") {
    val supervisorBehaviorTestKit = BehaviorTestKit(supervisorBehavior)

    val componentActor       = supervisorBehaviorTestKit.childInbox(componentTLAName).ref
    val pubSubLifecycleActor = supervisorBehaviorTestKit.childInbox(SupervisorBehavior.PubSubLifecycleActor).ref

    supervisorBehaviorTestKit.retrieveAllEffects() should contain(Watched(componentActor))
    supervisorBehaviorTestKit.retrieveAllEffects() should not contain Watched(pubSubLifecycleActor)
  }

  test("Supervisor should support concurrent updates to log-levels of components | DEOPSCSW-163") {
    val supervisor = typedSystem.systemActorOf(supervisorBehavior, "supervisor")

    val logMetadataProbe = TestProbe[LogMetadata]("log")
    import typedSystem.executionContext

    Future {
      supervisor ! SetComponentLogLevel(WARN)
      supervisor ! GetComponentLogMetadata(logMetadataProbe.ref)
    }
    Future {
      supervisor ! SetComponentLogLevel(WARN)
      supervisor ! GetComponentLogMetadata(logMetadataProbe.ref)
    }

    val logMetadata1 = logMetadataProbe.expectMessageType[LogMetadata]
    val logMetadata2 = logMetadataProbe.expectMessageType[LogMetadata]

    logMetadata1.componentLevel shouldBe WARN
    logMetadata2.componentLevel shouldBe WARN
  }

  // CSW-37: Add diagnosticMode handler to component handlers
  test("Supervisor should forward DiagnosticMode to componentTLA | DEOPSCSW-163, CSW-37") {
    val supervisorBehaviorTestKit = BehaviorTestKit(supervisorBehavior)

    val childInbox: TestInbox[RunningMessage] = supervisorBehaviorTestKit.childInbox(componentTLAName)
    val componentActor                        = childInbox.ref
    val startTime                             = UTCTime.now()
    val hint                                  = "engineering"

    val diagnosticModeMessage = DiagnosticMode(startTime, hint)

    supervisorBehaviorTestKit.run(Running(componentActor))
    supervisorBehaviorTestKit.run(diagnosticModeMessage)
    childInbox.receiveAll() should contain(diagnosticModeMessage)
  }

  private def createBehavior(timerScheduler: TimerScheduler[SupervisorMessage]): Behavior[ComponentMessage] = {

    Behaviors
      .setup[SupervisorMessage](ctx =>
        new SupervisorBehavior(
          ctx,
          timerScheduler,
          None,
          new SampleComponentBehaviorFactory,
          registrationFactory,
          cswCtx.copy(hcdInfo)
        )
      )
      .narrow
  }
}
