package csw.framework.internal.supervisor

import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.testkit.typed.scaladsl.TestProbe
import csw.common.components.framework.SampleComponentState.{choiceKey, initChoice, prefix}
import csw.common.utils.LockCommandFactory
import csw.framework.ComponentInfos.assemblyInfo
import csw.framework.FrameworkTestSuite
import csw.messages.commands.CommandResponse.{Accepted, NotAllowed}
import csw.messages.commands.{CommandName, CommandResponse, Setup}
import csw.messages.framework.LockingResponses._
import csw.messages.framework.{LifecycleStateChanged, LockingResponse, PubSub, SupervisorLifecycleState}
import csw.messages.params.models.{ObsId, Prefix}
import csw.messages.params.states.{CurrentState, StateName}
import csw.messages.scaladsl.CommandMessage.Submit
import csw.messages.scaladsl.{CommandResponseManagerMessage ⇒ CRM}
import CRM.{AddOrUpdateCommand, Query, Unsubscribe}
import csw.messages.scaladsl.ComponentCommonMessage.{ComponentStateSubscription, LifecycleStateSubscription}
import csw.messages.scaladsl.SupervisorLockMessage.{Lock, Unlock}
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.duration.DurationDouble

class SupervisorLockTest extends FrameworkTestSuite with BeforeAndAfterEach {

  //DEOPSCSW-222: Locking a component for a specific duration
  test("should able to lock and unlock a component") {
    val lockingStateProbe = TestProbe[LockingResponse]
    val mocks             = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Assure that component is in running state
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Client 1 will lock an assembly
    supervisorRef ! LockCommandFactory.make("wfos.prog.cloudcover.Client1", lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAcquired)

    // Client 2 tries to lock the assembly while Client 1 already has the lock
    supervisorRef ! LockCommandFactory.make("wfos.prog.cloudcover.Client2", lockingStateProbe.ref)
    lockingStateProbe.expectMessage(
      AcquiringLockFailed(
        s"Invalid source [WFOS, wfos.prog.cloudcover.Client2] for acquiring lock. Currently it is acquired by component: [WFOS, wfos.prog.cloudcover.Client1]"
      )
    )

    // Client 1 re-acquires the lock by sending the same token again
    supervisorRef ! LockCommandFactory.make("wfos.prog.cloudcover.Client1", lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAcquired)

    // Client 2 tries to unlock the assembly while Client 1 already has the lock
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client2", lockingStateProbe.ref)
    lockingStateProbe.expectMessage(
      ReleasingLockFailed(
        s"Invalid source [WFOS, wfos.prog.cloudcover.Client2] for releasing lock. Currently it is acquired by component: [WFOS, wfos.prog.cloudcover.Client1]"
      )
    )

    // Client 1 unlocks the assembly successfully
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client1", lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockReleased)

    // Client 1 tries to unlock the same assembly again while the lock is already released
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client1", lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAlreadyReleased)

    // Client 2 tries to unlock the same assembly while the lock is already released
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client2", lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAlreadyReleased)
  }

  // DEOPSCSW-222: Locking a component for a specific duration
  // DEOPSCSW-301: Support UnLocking
  test("should forward command messages from client that locked the component and reject for other clients ") {
    val lockingStateProbe    = TestProbe[LockingResponse]
    val commandResponseProbe = TestProbe[CommandResponse]

    val source1Prefix = Prefix("wfos.prog.cloudcover.source1")
    val commandName1  = CommandName("move.Client1.success")
    val source2Prefix = Prefix("wfos.prog.cloudcover.source2")
    val commandName2  = CommandName("move.Client2.success")
    val obsId         = ObsId("Obs001")

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
    supervisorRef ! Submit(Setup(source1Prefix, commandName1, Some(obsId)), commandResponseProbe.ref)
    commandResponseProbe.expectMessageType[Accepted]

    // Client 2 tries to send submit command while Client 1 has the lock
    supervisorRef ! Submit(Setup(source2Prefix, commandName2, Some(obsId)), commandResponseProbe.ref)
    commandResponseProbe.expectMessageType[NotAllowed]

    // Client 1 unlocks the assembly
    supervisorRef ! Unlock(source1Prefix, lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockReleased)

    // Client 2 tries to send submit command again after lock is released
    supervisorRef ! Submit(Setup(source2Prefix, commandName2, Some(obsId)), commandResponseProbe.ref)
    commandResponseProbe.expectMessageType[Accepted]

    // Client 2 will lock an assembly
    supervisorRef ! LockCommandFactory.make("wfos.prog.cloudcover.Client2", lockingStateProbe.ref)
    lockingStateProbe.expectMessage(LockAcquired)

    // Client 1 tries to send submit command while Client 2 has the lock
    supervisorRef ! Submit(Setup(source1Prefix, commandName1, Some(obsId)), commandResponseProbe.ref)
    commandResponseProbe.expectMessageType[NotAllowed]
  }

  // DEOPSCSW-222: Locking a component for a specific duration
  // DEOPSCSW-301: Support UnLocking
  test("should forward messages that are of type SupervisorLockMessage to TLA") {
    val lockingStateProbe    = TestProbe[LockingResponse]
    val commandResponseProbe = TestProbe[CommandResponse]()(untypedSystem.toTyped)

    val sourcePrefix = Prefix("wfos.prog.cloudcover.source")
    val commandName  = CommandName("move.Client1.success")
    val obsId        = ObsId("Obs001")

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
    supervisorRef ! Submit(setup, commandResponseProbe.ref)
    commandResponseProbe.expectMessageType[Accepted]
    commandResponseManagerActor.expectMessage(AddOrUpdateCommand(setup.runId, Accepted(setup.runId)))

    // Ensure Query can be sent to component even in locked state
    supervisorRef ! Query(setup.runId, commandResponseProbe.ref)
    commandResponseManagerActor.expectMessage(Query(setup.runId, commandResponseProbe.ref))

    // Ensure Subscribe can be sent to component even in locked state
    supervisorRef ! CRM.Subscribe(setup.runId, commandResponseProbe.ref)
    commandResponseManagerActor.expectMessage(CRM.Subscribe(setup.runId, commandResponseProbe.ref))

    // Ensure Unsubscribe can be sent to component even in locked state
    supervisorRef ! Unsubscribe(setup.runId, commandResponseProbe.ref)
    // to prove un-subscribe is handled, sending a same setup command with the same runId again
    // now that we have un-subscribed, commandResponseProbe is not expecting command completion result (validation ll be received)
    supervisorRef ! Submit(setup, commandResponseProbe.ref)
    commandResponseProbe.expectMessageType[Accepted]
    commandResponseProbe.expectNoMessage(200.millis)
  }

  // DEOPSCSW-223 Expiry of component Locking mode
  test("should expire lock after timeout") {
    val lockingStateProbe    = TestProbe[LockingResponse]
    val commandResponseProbe = TestProbe[CommandResponse]

    val source1Prefix = Prefix("wfos.prog.cloudcover.Client1.success")
    val source2Prefix = Prefix("wfos.prog.cloudcover.source2.success")
    val commandName   = CommandName("success.move")
    val obsId         = ObsId("Obs001")

    val mocks = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    supervisorRef ! ComponentStateSubscription(PubSub.Subscribe(compStateProbe.ref))
    supervisorRef ! LifecycleStateSubscription(PubSub.Subscribe(lifecycleStateProbe.ref))

    // Assure that component is in running state
    compStateProbe.expectMessage(CurrentState(prefix, StateName("testStateName"), Set(choiceKey.set(initChoice))))
    lifecycleStateProbe.expectMessage(LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running))

    // Client 1 will lock an assembly
    supervisorRef ! Lock(source1Prefix, lockingStateProbe.ref, 100.millis)
    lockingStateProbe.expectMessage(LockAcquired)
    lockingStateProbe.expectMessage(LockExpiringShortly)

    // Client 2 tries to send submit command while Client 1 has the lock
    supervisorRef ! Submit(Setup(source2Prefix, commandName, Some(obsId)), commandResponseProbe.ref)
    commandResponseProbe.expectMessageType[NotAllowed]

    // Reacquire lock before it gets expired
    supervisorRef ! Lock(source1Prefix, lockingStateProbe.ref, 100.millis)
    lockingStateProbe.expectMessage(LockAcquired)

    // this is to prove that timeout gets reset after renewing lock
    lockingStateProbe.expectNoMessage(50.millis)
    lockingStateProbe.expectMessage(LockExpiringShortly)
    lockingStateProbe.expectMessage(LockExpired)

    // Client 2 tries to send submit command again after lock is released
    supervisorRef ! Submit(Setup(source2Prefix, commandName, Some(obsId)), commandResponseProbe.ref)
    commandResponseProbe.expectMessageType[Accepted]
  }

  // DEOPSCSW-223 Expiry of component Locking mode
  test("should not publish LockExpired or LockExpiringShortly messages if component is unlocked within timeout") {
    val lockingStateProbe = TestProbe[LockingResponse]
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
