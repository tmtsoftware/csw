package csw.framework.internal.supervisor

import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.typed.testkit.scaladsl.TestProbe
import csw.common.components.framework.ComponentStatistics
import csw.common.components.framework.SampleComponentState.{choiceKey, domainChoice, initChoice, prefix}
import csw.common.utils.LockCommandFactory
import csw.framework.ComponentInfos.assemblyInfo
import csw.framework.FrameworkTestSuite
import csw.messages.CommandMessage.Submit
import csw.messages.CommandResponseManagerMessage.{Query, Subscribe, Unsubscribe}
import csw.messages.SupervisorLockMessage.{Lock, Unlock}
import csw.messages.ccs.commands.CommandResponse.{Accepted, Completed, NotAllowed}
import csw.messages.ccs.commands.{CommandResponse, Setup}
import csw.messages.framework.SupervisorLifecycleState
import csw.messages.models
import csw.messages.models.LockingResponse
import csw.messages.models.LockingResponse._
import csw.messages.models.PubSub.Publish
import csw.messages.params.models.{ObsId, Prefix}
import csw.messages.params.states.CurrentState
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.duration.DurationDouble

class SupervisorLockTest extends FrameworkTestSuite with BeforeAndAfterEach {

  //DEOPSCSW-222: Locking a component for a specific duration
  test("should able to lock and unlock a component") {
    val lockingStateProbe = TestProbe[LockingResponse]
    val mocks             = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    // Assure that component is in running state
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    lifecycleStateProbe.expectMsg(Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // Client 1 will lock an assembly
    supervisorRef ! LockCommandFactory.make("wfos.prog.cloudcover.Client1", lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAcquired)

    // Client 2 tries to lock the assembly while Client 1 already has the lock
    supervisorRef ! LockCommandFactory.make("wfos.prog.cloudcover.Client2", lockingStateProbe.ref)
    lockingStateProbe.expectMsg(
      AcquiringLockFailed(
        s"Invalid prefix [WFOS, wfos.prog.cloudcover.Client2] for acquiring lock. Currently it is acquired by component: [WFOS, wfos.prog.cloudcover.Client1]"
      )
    )

    // Client 1 re-acquires the lock by sending the same token again
    supervisorRef ! LockCommandFactory.make("wfos.prog.cloudcover.Client1", lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAcquired)

    // Client 2 tries to unlock the assembly while Client 1 already has the lock
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client2", lockingStateProbe.ref)
    lockingStateProbe.expectMsg(
      ReleasingLockFailed(
        s"Invalid prefix [WFOS, wfos.prog.cloudcover.Client2] for releasing lock. Currently it is acquired by component: [WFOS, wfos.prog.cloudcover.Client1]"
      )
    )

    // Client 1 unlocks the assembly successfully
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client1", lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockReleased)

    // Client 1 tries to unlock the same assembly again while the lock is already released
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client1", lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAlreadyReleased)

    // Client 2 tries to unlock the same assembly while the lock is already released
    supervisorRef ! Unlock("wfos.prog.cloudcover.Client2", lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAlreadyReleased)
  }

  // DEOPSCSW-222: Locking a component for a specific duration
  // DEOPSCSW-301: Support UnLocking
  test("should forward command messages from client that locked the component and reject for other clients ") {
    val lockingStateProbe    = TestProbe[LockingResponse]
    val commandResponseProbe = TestProbe[CommandResponse]

    val client1Prefix = Prefix("wfos.prog.cloudcover.Client1.success")
    val client2Prefix = Prefix("wfos.prog.cloudcover.Client2.success")
    val obsId         = ObsId("Obs001")

    val mocks = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    // Assure that component is in running state
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    lifecycleStateProbe.expectMsg(Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // Client 1 will lock an assembly
    supervisorRef ! LockCommandFactory.make(client1Prefix, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAcquired)

    // Client 1 sends submit command with tokenId in parameter set
    supervisorRef ! Submit(Setup(obsId, client1Prefix), commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[Accepted]

    // Client 2 tries to send submit command while Client 1 has the lock
    supervisorRef ! Submit(Setup(obsId, client2Prefix), commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[NotAllowed]

    // Client 1 unlocks the assembly
    supervisorRef ! Unlock(client1Prefix, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockReleased)

    // Client 2 tries to send submit command again after lock is released
    supervisorRef ! Submit(Setup(obsId, client2Prefix), commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[Accepted]
  }

  // DEOPSCSW-222: Locking a component for a specific duration
  // DEOPSCSW-301: Support UnLocking
  test("should forward messages that are of type SupervisorLockMessage to TLA") {
    val lockingStateProbe    = TestProbe[LockingResponse]
    val commandResponseProbe = TestProbe[CommandResponse]()(untypedSystem.toTyped, settings)

    val client1Prefix = Prefix("wfos.prog.cloudcover.Client1.success")
    val obsId         = ObsId("Obs001")

    val mocks = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    // Assure that component is in running state
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    lifecycleStateProbe.expectMsg(Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // Client 1 will lock an assembly
    supervisorRef ! LockCommandFactory.make(client1Prefix, lockingStateProbe.ref)
    lockingStateProbe.expectMsg(LockAcquired)

    // Ensure Domain messages can be sent to component even in locked state
    supervisorRef ! ComponentStatistics(1)
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(domainChoice)))))

    // Client 1 sends submit command with tokenId in parameter set
    val setup = Setup(obsId, client1Prefix)
    supervisorRef ! Submit(setup, commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[Accepted]

    // Ensure Query can be sent to component even in locked state
    supervisorRef ! Query(setup.runId, commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[CommandResponse]

    // Ensure Subscribe can be sent to component even in locked state
    supervisorRef ! Subscribe(setup.runId, commandResponseProbe.ref)
    commandResponseProbe.expectMsg(Completed(setup.runId))

    // Ensure Unsubscribe can be sent to component even in locked state
    supervisorRef ! Unsubscribe(setup.runId, commandResponseProbe.ref)
    // to prove un-subscribe is handled, sending a same setup command with the same runId again
    // now that we have un-subscribed, commandResponseProbe is not expecting command completion result (validation ll be received)
    supervisorRef ! Submit(setup, commandResponseProbe.ref)
    commandResponseProbe.expectMsgType[Accepted]
    commandResponseProbe.expectNoMsg(200.millis)
  }

  test("should expire lock after timeout") {
    val lockingStateProbe = TestProbe[LockingResponse]
    val client1Prefix     = Prefix("wfos.prog.cloudcover.Client1.success")

    val mocks = frameworkTestMocks()
    import mocks._

    val supervisorRef = createSupervisorAndStartTLA(assemblyInfo, mocks)

    // Assure that component is in running state
    compStateProbe.expectMsg(Publish(CurrentState(prefix, Set(choiceKey.set(initChoice)))))
    lifecycleStateProbe.expectMsg(Publish(models.LifecycleStateChanged(supervisorRef, SupervisorLifecycleState.Running)))

    // Client 1 will lock an assembly
    supervisorRef ! Lock(client1Prefix, lockingStateProbe.ref, 100.millis)
    lockingStateProbe.expectMsg(LockAcquired)
    lockingStateProbe.expectMsg(200.millis, LockExpired)
  }
}
