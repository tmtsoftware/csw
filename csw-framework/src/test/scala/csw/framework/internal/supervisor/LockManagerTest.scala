package csw.framework.internal.supervisor

import akka.actor.{typed, ActorSystem}
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.messages.commands.CommandIssue.ComponentLockedIssue
import csw.messages.commands.CommandResponse.NotAllowed
import csw.messages.commands.{CommandName, CommandResponse, Setup}
import csw.messages.framework.LockingResponse
import csw.messages.framework.LockingResponses._
import csw.messages.params.generics.{KeyType, Parameter}
import csw.messages.params.models.{ObsId, Prefix}
import csw.messages.CommandMessage.Submit
import csw.services.logging.scaladsl.{Logger, LoggerFactory}
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-222: Locking a component for a specific duration
// DEOPSCSW-301: Support UnLocking
class LockManagerTest extends FunSuite with MockitoSugar with Matchers {

  private val prefix        = Prefix("tcs.mobie.blue.filter")
  private val invalidPrefix = Prefix("tcs.mobie.blue.filter.invalid")

  implicit val system: ActorSystem                     = ActorSystem()
  implicit val typedSystem: typed.ActorSystem[Nothing] = system.toTyped
  implicit val testKitSettings: TestKitSettings        = TestKitSettings(typedSystem)

  private val intParam: Parameter[Int] = KeyType.IntKey.make("intKey").set(1, 2, 3)
  private val setup: Setup             = Setup(prefix, CommandName("move"), Some(ObsId("obs1001")), Set(intParam))
  private val invalidSetup: Setup      = Setup(invalidPrefix, CommandName("move"), Some(ObsId("obs1001")), Set(intParam))

  private val mockedLoggerFactory = mock[LoggerFactory]
  private val mockedLogger        = mock[Logger]
  when(mockedLoggerFactory.getLogger).thenReturn(mockedLogger)

  test("should be locked when prefix is available") {
    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true
    lockManager.isUnLocked shouldBe false
  }

  test("should be unlocked when prefix is not available") {
    val lockManager = new LockManager(None, mockedLoggerFactory)
    lockManager.isLocked shouldBe false
    lockManager.isUnLocked shouldBe true
  }

  test("should able to lock") {
    val lockingResponseProbe = TestProbe[LockingResponse]

    val lockManager = new LockManager(None, mockedLoggerFactory)
    lockManager.isLocked shouldBe false

    val updatedLockManager = lockManager.lockComponent(prefix, lockingResponseProbe.ref)(Unit)
    lockingResponseProbe.expectMessage(LockAcquired)
    updatedLockManager.isLocked shouldBe true
  }

  test("should able to reacquire lock") {
    val lockingResponseProbe = TestProbe[LockingResponse]

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    val updatedLockManager = lockManager.lockComponent(prefix, lockingResponseProbe.ref)(Unit)
    lockingResponseProbe.expectMessage(LockAcquired)
    updatedLockManager.isLocked shouldBe true
  }

  test("should not acquire lock when invalid prefix is provided") {
    val lockingResponseProbe = TestProbe[LockingResponse]

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    val updatedLockManager = lockManager.lockComponent(invalidPrefix, lockingResponseProbe.ref)(Unit)
    lockingResponseProbe.expectMessageType[AcquiringLockFailed]
    updatedLockManager.isLocked shouldBe true
    updatedLockManager.lockPrefix.get shouldBe prefix
  }

  test("should able to unlock") {
    val lockingResponseProbe = TestProbe[LockingResponse]

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isUnLocked shouldBe false

    val updatedLockManager = lockManager.unlockComponent(prefix, lockingResponseProbe.ref)(Unit)
    lockingResponseProbe.expectMessage(LockReleased)
    updatedLockManager.isUnLocked shouldBe true
  }

  test("should not able to unlock with invalid prefix") {
    val lockingResponseProbe = TestProbe[LockingResponse]

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isUnLocked shouldBe false

    val updatedLockManager = lockManager.unlockComponent(invalidPrefix, lockingResponseProbe.ref)(Unit)
    lockingResponseProbe.expectMessageType[ReleasingLockFailed]
    updatedLockManager.isUnLocked shouldBe false
  }

  test("should not result into failure when tried to unlock already unlocked component") {
    val lockingResponseProbe = TestProbe[LockingResponse]

    val lockManager = new LockManager(None, mockedLoggerFactory)
    lockManager.isUnLocked shouldBe true

    val updatedLockManager = lockManager.unlockComponent(prefix, lockingResponseProbe.ref)(Unit)
    lockingResponseProbe.expectMessage(LockAlreadyReleased)
    updatedLockManager.isUnLocked shouldBe true
  }

  test("should allow commands when component is not locked") {
    val commandResponseProbe = TestProbe[CommandResponse]

    val lockManager = new LockManager(None, mockedLoggerFactory)
    lockManager.isUnLocked shouldBe true

    lockManager.allowCommand(Submit(setup, commandResponseProbe.ref)) shouldBe true
  }

  test("should allow commands when component is locked with same prefix") {
    val commandResponseProbe = TestProbe[CommandResponse]

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    lockManager.allowCommand(Submit(setup, commandResponseProbe.ref)) shouldBe true
  }

  test("should not allow commands when component is locked with different prefix") {
    val commandResponseProbe = TestProbe[CommandResponse]

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    lockManager.allowCommand(Submit(invalidSetup, commandResponseProbe.ref)) shouldBe false
    val commandResponse = commandResponseProbe.expectMessageType[NotAllowed]
    commandResponse.issue shouldBe a[ComponentLockedIssue]
  }
}
