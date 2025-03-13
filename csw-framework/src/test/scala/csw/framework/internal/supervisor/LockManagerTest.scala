/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.internal.supervisor

import org.apache.pekko.actor.testkit.typed.TestKitSettings
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.scaladsl.adapter.*
import org.apache.pekko.actor.{ActorSystem, typed}
import csw.command.client.messages.CommandMessage.Submit
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.LockingResponse.*
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Setup}
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.ObsId
import csw.prefix.models.{Prefix, Subsystem}
import org.mockito.Mockito.when
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

// DEOPSCSW-222: Locking a component for a specific duration
// DEOPSCSW-301: Support UnLocking
class LockManagerTest extends AnyFunSuite with MockitoSugar with Matchers {

  private val prefix        = Prefix("wfos.blue.filter")
  private val invalidPrefix = Prefix("wfos.blue.filter.invalid")

  implicit val system: ActorSystem                     = ActorSystem()
  implicit val typedSystem: typed.ActorSystem[Nothing] = system.toTyped
  implicit val testKitSettings: TestKitSettings        = TestKitSettings(typedSystem)

  private val intParam: Parameter[Int] = KeyType.IntKey.make("intKey").set(1, 2, 3)
  private val setup: Setup             = Setup(prefix, CommandName("move"), Some(ObsId("2020A-001-123")), Set(intParam))
  private val invalidSetup: Setup      = Setup(invalidPrefix, CommandName("move"), Some(ObsId("2020A-001-123")), Set(intParam))
  private val mockedLoggerFactory      = mock[LoggerFactory]
  private val mockedLogger             = mock[Logger]
  when(mockedLoggerFactory.getLogger).thenReturn(mockedLogger)

  test("should be locked when prefix is available | DEOPSCSW-222, DEOPSCSW-301") {
    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true
    lockManager.isUnLocked shouldBe false
  }

  test("should be unlocked when prefix is not available | DEOPSCSW-222, DEOPSCSW-301") {
    val lockManager = new LockManager(None, mockedLoggerFactory)
    lockManager.isLocked shouldBe false
    lockManager.isUnLocked shouldBe true
  }

  test("should able to lock | DEOPSCSW-222, DEOPSCSW-301") {
    val lockingResponseProbe = TestProbe[LockingResponse]()

    val lockManager = new LockManager(None, mockedLoggerFactory)
    lockManager.isLocked shouldBe false

    val updatedLockManager = lockManager.lockComponent(prefix, lockingResponseProbe.ref)(())
    lockingResponseProbe.expectMessage(LockAcquired)
    updatedLockManager.isLocked shouldBe true
  }

  test("should able to reacquire lock | DEOPSCSW-222, DEOPSCSW-301") {
    val lockingResponseProbe = TestProbe[LockingResponse]()

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    val updatedLockManager = lockManager.lockComponent(prefix, lockingResponseProbe.ref)(())
    lockingResponseProbe.expectMessage(LockAcquired)
    updatedLockManager.isLocked shouldBe true
  }

  test("should not acquire lock when invalid prefix is provided | DEOPSCSW-222, DEOPSCSW-301") {
    val lockingResponseProbe = TestProbe[LockingResponse]()

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    val updatedLockManager = lockManager.lockComponent(invalidPrefix, lockingResponseProbe.ref)(())
    lockingResponseProbe.expectMessageType[AcquiringLockFailed]
    updatedLockManager.isLocked shouldBe true
    updatedLockManager.lockPrefix.get shouldBe prefix
  }

  test("should able to unlock | DEOPSCSW-222, DEOPSCSW-301") {
    val lockingResponseProbe = TestProbe[LockingResponse]()

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isUnLocked shouldBe false

    val updatedLockManager = lockManager.unlockComponent(prefix, lockingResponseProbe.ref)(())
    lockingResponseProbe.expectMessage(LockReleased)
    updatedLockManager.isUnLocked shouldBe true
  }

  test("should not able to unlock with invalid prefix | DEOPSCSW-222, DEOPSCSW-301") {
    val lockingResponseProbe = TestProbe[LockingResponse]()

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isUnLocked shouldBe false

    val updatedLockManager = lockManager.unlockComponent(invalidPrefix, lockingResponseProbe.ref)(())
    lockingResponseProbe.expectMessageType[ReleasingLockFailed]
    updatedLockManager.isUnLocked shouldBe false
  }

  test("should not result into failure when tried to unlock already unlocked component | DEOPSCSW-222, DEOPSCSW-301") {
    val lockingResponseProbe = TestProbe[LockingResponse]()

    val lockManager = new LockManager(None, mockedLoggerFactory)
    lockManager.isUnLocked shouldBe true

    val updatedLockManager = lockManager.unlockComponent(prefix, lockingResponseProbe.ref)(())
    lockingResponseProbe.expectMessage(LockAlreadyReleased)
    updatedLockManager.isUnLocked shouldBe true
  }

  test("should allow commands when component is not locked | DEOPSCSW-222, DEOPSCSW-301") {
    val commandResponseProbe = TestProbe[SubmitResponse]()

    val lockManager = new LockManager(None, mockedLoggerFactory)
    lockManager.isUnLocked shouldBe true

    lockManager.allowCommand(Submit(setup, commandResponseProbe.ref)) shouldBe true
  }

  test("should allow commands when component is locked with same prefix | DEOPSCSW-222, DEOPSCSW-301") {
    val commandResponseProbe = TestProbe[SubmitResponse]()

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    lockManager.allowCommand(Submit(setup, commandResponseProbe.ref)) shouldBe true
  }

  test("should not allow commands when component is locked with different prefix | DEOPSCSW-222, DEOPSCSW-301") {
    val commandResponseProbe = TestProbe[SubmitResponse]()

    val lockManager = new LockManager(Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    lockManager.allowCommand(Submit(invalidSetup, commandResponseProbe.ref)) shouldBe false
  }

  // DEOPSCSW-302: Support Unlocking by Admin
  test("should allow unlocking any locked component by admin | DEOPSCSW-222, DEOPSCSW-301, DEOPSCSW-302") {
    val replyTo        = TestProbe[SubmitResponse]().ref
    val adminPrefix    = Prefix(s"${Subsystem.CSW}.admin")
    val nonAdminPrefix = Prefix(s"${Subsystem.CSW}.NonAdmin")

    val lockManager = new LockManager(lockPrefix = Some(prefix), mockedLoggerFactory)
    lockManager.isLocked shouldBe true

    val nonAdminCmd: Setup = Setup(nonAdminPrefix, CommandName("move"), Some(ObsId("2020A-001-123")), Set(intParam))
    lockManager.allowCommand(Submit(nonAdminCmd, replyTo)) shouldBe false

    val probe               = TestProbe[LockingResponse]()
    val unlockedLockManager = lockManager.unlockComponent(adminPrefix, probe.ref) {}
    probe.expectMessage(LockReleased)

    unlockedLockManager.allowCommand(Submit(nonAdminCmd, replyTo)) shouldBe true
  }
}
