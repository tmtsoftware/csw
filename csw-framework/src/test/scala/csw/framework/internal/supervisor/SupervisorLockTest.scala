/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.supervisor

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import csw.command.client.messages.CommandMessage.Submit
import csw.command.client.messages.ComponentCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.command.client.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.command.client.models.framework.LockingResponse.*
import csw.command.client.models.framework.{LifecycleStateChanged, LockingResponse, PubSub, SupervisorLifecycleState}
import csw.common.components.framework.SampleComponentState.{choiceKey, initChoice, prefix}
import csw.common.utils.LockCommandFactory
import csw.framework.ComponentInfos.assemblyInfo
import csw.framework.FrameworkTestSuite
import csw.params.commands.CommandResponse.*
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.ObsId
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.Prefix
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.duration.DurationDouble

class SupervisorLockTest extends FrameworkTestSuite with BeforeAndAfterEach {

  // DEOPSCSW-222: Locking a component for a specific duration
  // CSW-86: Subsystem should be case-insensitive
  test("should able to lock and unlock a component | DEOPSCSW-222") {
    val lockingStateProbe = TestProbe[LockingResponse]()
    val mocks             = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Assure that component is in running state
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Client 1 will lock an assembly
    supervisorRef ! LockCommandFactory.make(Prefix("wfos.prog.cloudcover.Client1"), lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAcquired)

    // Client 2 tries to lock the assembly while Client 1 already has the lock
    supervisorRef ! LockCommandFactory.make(Prefix("wfos.prog.cloudcover.Client2"), lockingStateProbe.ref)
    lockingStateProbe.expectMessage(
      AcquiringLockFailed(
        s"Invalid source WFOS.prog.cloudcover.Client2 for acquiring lock. Currently it is acquired by component: WFOS.prog.cloudcover.Client1"
      )
    )

    // Client 1 re-acquires the lock by sending the same token again
    supervisorRef ! LockCommandFactory.make(Prefix("wfos.prog.cloudcover.Client1"), lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAcquired)

    // Client 2 tries to unlock the assembly while Client 1 already has the lock
    supervisorRef ! Unlock(Prefix("wfos.prog.cloudcover.Client2"), lockingStateProbe.ref)
    lockingStateProbe.expectMessage(
      ReleasingLockFailed(
        s"Invalid source WFOS.prog.cloudcover.Client2 for releasing lock. Currently it is acquired by component: WFOS.prog.cloudcover.Client1"
      )
    )

    // Client 1 unlocks the assembly successfully
    supervisorRef ! Unlock(Prefix("wfos.prog.cloudcover.Client1"), lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockReleased)

    // Client 1 tries to unlock the same assembly again while the lock is already released
    supervisorRef ! Unlock(Prefix("wfos.prog.cloudcover.Client1"), lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAlreadyReleased)

    // Client 2 tries to unlock the same assembly while the lock is already released
    supervisorRef ! Unlock(Prefix("wfos.prog.cloudcover.Client2"), lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAlreadyReleased)
  }

  // DEOPSCSW-222: Locking a component for a specific duration
  // DEOPSCSW-301: Support UnLocking
  test(
    "should forward command messages from client that locked the component and reject for other clients  | DEOPSCSW-222, DEOPSCSW-301"
  ) {
    val lockingStateProbe   = TestProbe[LockingResponse]()
    val submitResponseProbe = TestProbe[SubmitResponse]()

    val source1Prefix = Prefix("wfos.prog.cloudcover.source1")
    val commandName1  = CommandName("move.Client1.success")
    val source2Prefix = Prefix("wfos.prog.cloudcover.source2")
    val commandName2  = CommandName("move.Client2.success")
    val obsId         = ObsId("2020A-001-123")

    val mocks = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Assure that component is in running state
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Client 1 will lock an assembly
    supervisorRef ! LockCommandFactory.make(source1Prefix, lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAcquired)

    // Client 1 sends submit command with tokenId in parameter set
    supervisorRef ! Submit(Setup(source1Prefix, commandName1, Some(obsId)), submitResponseProbe.ref)
    submitResponseProbe.expectMessageType[Completed]

    // Client 2 tries to send submit command while Client 1 has the lock
    supervisorRef ! Submit(Setup(source2Prefix, commandName2, Some(obsId)), submitResponseProbe.ref)
    submitResponseProbe.expectMessageType[Locked]

    // Client 1 unlocks the assembly
    supervisorRef ! Unlock(source1Prefix, lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockReleased)

    // Client 2 tries to send submit command again after lock is released
    supervisorRef ! Submit(Setup(source2Prefix, commandName2, Some(obsId)), submitResponseProbe.ref)
    submitResponseProbe.expectMessageType[Completed]

    // Client 2 will lock an assembly
    supervisorRef ! LockCommandFactory.make(Prefix("wfos.prog.cloudcover.Client2"), lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAcquired)

    // Client 1 tries to send submit command while Client 2 has the lock
    supervisorRef ! Submit(Setup(source1Prefix, commandName1, Some(obsId)), submitResponseProbe.ref)
    submitResponseProbe.expectMessageType[Locked]
  }

  // DEOPSCSW-222: Locking a component for a specific duration
  // DEOPSCSW-301: Support UnLocking
  test("should forward messages that are of type SupervisorLockMessage to TLA | DEOPSCSW-222, DEOPSCSW-301") {
    val lockingStateProbe  = TestProbe[LockingResponse]()
    val queryResponseProbe = TestProbe[SubmitResponse]()(typedSystem)

    val sourcePrefix = Prefix("wfos.prog.cloudcover.source")
    val commandName  = CommandName("move.Client1.success")
    val obsId        = ObsId("2020A-001-123")

    val mocks = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Assure that component is in running state
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Client 1 will lock an assembly
    supervisorRef ! LockCommandFactory.make(sourcePrefix, lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAcquired)

    // Client 1 sends submit command with tokenId in parameter set
    val setup = Setup(sourcePrefix, commandName, Some(obsId))
    supervisorRef ! Submit(setup, queryResponseProbe.ref)
    queryResponseProbe.expectMessageType[Completed]
  }

  // DEOPSCSW-223 Expiry of component Locking mode
  test("should expire lock after timeout | DEOPSCSW-223") {
    val lockingStateProbe   = TestProbe[LockingResponse]()
    val submitResponseProbe = TestProbe[SubmitResponse]()

    val source1Prefix = Prefix("wfos.prog.cloudcover.Client1.success")
    val source2Prefix = Prefix("wfos.prog.cloudcover.source2.success")
    val commandName   = CommandName("success.move")
    val obsId         = ObsId("2020A-001-123")

    val mocks = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Assure that component is in running state
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Client 1 will lock an assembly - Note this delay must be long enough to allow client 2 to reject
    supervisorRef ! Lock(source1Prefix, lockingStateProbe.ref, 200.millis)
    lockingStateProbe.expectMessage(LockAcquired)
    lockingStateProbe.expectMessage(LockExpiringShortly)

    // Client 2 tries to send submit command while Client 1 has the lock
    supervisorRef ! Submit(Setup(source2Prefix, commandName, Some(obsId)), submitResponseProbe.ref)
    submitResponseProbe.expectMessageType[Locked]

    // Reacquire lock before it gets expired
    supervisorRef ! Lock(source1Prefix, lockingStateProbe.ref, 100.millis)
    lockingStateProbe.expectMessage(LockAcquired)

    // this is to prove that timeout gets reset after renewing lock
    lockingStateProbe.expectNoMessage(50.millis)
    lockingStateProbe.expectMessage(LockExpiringShortly)
    lockingStateProbe.expectMessage(LockExpired)

    // Client 2 tries to send submit command again after lock is released
    supervisorRef ! Submit(Setup(source2Prefix, commandName, Some(obsId)), submitResponseProbe.ref)
    submitResponseProbe.expectMessageType[Completed]
  }

  // DEOPSCSW-223 Expiry of component Locking mode
  test("should not publish LockExpired or LockExpiringShortly messages if component is unlocked within timeout | DEOPSCSW-223") {
    val lockingStateProbe = TestProbe[LockingResponse]()
    val client1Prefix     = Prefix("wfos.prog.cloudcover.Client1.success")

    val mocks = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Assure that component is in running state
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Client 1 will lock an assembly
    supervisorRef ! Lock(client1Prefix, lockingStateProbe.ref, 100.millis)
    lockingStateProbe.expectMessage(LockAcquired)
    supervisorRef ! Unlock(client1Prefix, lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockReleased)
    lockingStateProbe.expectNoMessage(100.millis)
  }
}
