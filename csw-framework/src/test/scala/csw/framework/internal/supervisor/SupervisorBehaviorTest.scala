/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.supervisor

import akka.actor.testkit.typed.Effect
import akka.actor.testkit.typed.Effect.{Spawned, Watched}
import akka.actor.testkit.typed.scaladsl.{BehaviorTestKit, TestInbox, TestProbe}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.command.client.messages.FromComponentLifecycleMessage.Running
import csw.command.client.messages.*
import csw.common.components.framework.SampleComponentHandlers
import csw.common.extensions.CswContextExtensions.RichCswContext
import csw.framework.ComponentInfos.*
import csw.framework.{FrameworkTestMocks, FrameworkTestSuite}
import csw.logging.models.Level.WARN
import csw.logging.models.LogMetadata
import csw.time.core.models.UTCTime
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{doNothing, when}
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

// DEOPSCSW-163: Provide admin facilities in the framework through Supervisor role
class SupervisorBehaviorTest extends FrameworkTestSuite with MockitoSugar {
  val testMocks: FrameworkTestMocks = frameworkTestMocks()
  import testMocks.*

  val containerIdleMessageProbe: TestProbe[ContainerIdleMessage] = TestProbe[ContainerIdleMessage]()
  private val timerScheduler                                     = mock[TimerScheduler[SupervisorMessage]]

  doNothing()
    .when(timerScheduler)
    .startSingleTimer(
      ArgumentMatchers.eq(SupervisorBehavior.InitializeTimerKey),
      ArgumentMatchers.any[SupervisorMessage],
      ArgumentMatchers.any[FiniteDuration]
    )

  doNothing().when(timerScheduler).cancel(ArgumentMatchers.eq(SupervisorBehavior.InitializeTimerKey))
  when(timerScheduler.isTimerActive(SupervisorBehavior.InitializeTimerKey)).thenReturn(true)

  val supervisorBehavior: Behavior[ComponentMessage] = createBehavior(timerScheduler)
  val componentTLAName                               = s"${hcdInfo.prefix}-${SupervisorBehavior.ComponentActorNameSuffix}"

  test("Supervisor should create child actors for TLA, pub-sub actor for lifecycle and component state | DEOPSCSW-163") {
    val supervisorBehaviorTestKit = BehaviorTestKit(supervisorBehavior)

    val effects: immutable.Seq[Effect] = supervisorBehaviorTestKit.retrieveAllEffects()

    val spawnedEffects = effects.map {
      case s: Spawned[?] => s.childName
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
          (ctx, cswCtx) => new SampleComponentHandlers(ctx, cswCtx),
          registrationFactory,
          cswCtx.copy(hcdInfo)
        )
      )
      .narrow
  }
}
