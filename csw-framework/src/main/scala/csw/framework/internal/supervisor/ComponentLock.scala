package csw.framework.internal.supervisor

import akka.typed.ActorRef
import csw.messages.CommandMessage
import csw.messages.ccs.CommandIssue.{ComponentLockedIssue, UnsupportedCommandInStateIssue}
import csw.messages.ccs.commands.CommandResponse.{Invalid, NotAllowed}
import csw.messages.models.LockingResponse
import csw.messages.models.LockingResponse._
import csw.services.logging.scaladsl.FrameworkLogger

case class ComponentLock(_prefix: String, _token: String)
class LockManager(val lock: Option[ComponentLock], _componentName: String) extends FrameworkLogger.Simple {

  def lockComponent(prefix: String, token: String, replyTo: ActorRef[LockingResponse]): LockManager = lock match {
    case None                                   ⇒ onAcquiringLock(prefix, token, replyTo)
    case Some(ComponentLock(`prefix`, `token`)) ⇒ onReAcquiringLock(prefix, replyTo)
    case Some(ComponentLock(currentPrefix, _))  ⇒ onReAcquiringFailed(replyTo, prefix, token, currentPrefix)
  }

  def unlockComponent(prefix: String, token: String, replyTo: ActorRef[LockingResponse]): LockManager = lock match {
    case Some(ComponentLock(`prefix`, `token`)) ⇒ onLockReleased(prefix, replyTo)
    case Some(ComponentLock(currentPrefix, _))  ⇒ onLockReleaseFailed(replyTo, prefix, token, currentPrefix)
    case _                                      ⇒ onLockAlreadyReleased(prefix, replyTo)
  }

  def allowCommand(commandMessage: CommandMessage): Boolean = lock match {
    case None ⇒ true
    case Some(ComponentLock(prefix, lockToken)) ⇒
      val command = commandMessage.command
      command.getLockToken match {
        case Some(`lockToken`) ⇒
          log.info(s"Forwarding message [${commandMessage.toString}]")
          true
        case _ ⇒
          log.error(s"Cannot process the command [${command.toString}] as the lock is acquired by component: [$prefix]")
          commandMessage.replyTo ! NotAllowed(
            command.runId,
            ComponentLockedIssue(s"This component is locked by component [$prefix]")
          )
          false
      }
  }

  private def onAcquiringLock(prefix: String, token: String, replyTo: ActorRef[LockingResponse]): LockManager = {
    log.info(s"The lock is successfully acquired by component: [$prefix]")
    replyTo ! LockAcquired
    new LockManager(Some(ComponentLock(prefix, token)), this.componentName()) //TODO: Start the timer for lock lease
  }

  private def onReAcquiringLock(prefix: String, replyTo: ActorRef[LockingResponse]): LockManager = {
    log.info(s"The lock is re-acquired by component: [$prefix]") //TODO: re-start the timer for lock lease
    replyTo ! LockAcquired
    this
  }

  private def onReAcquiringFailed(
      replyTo: ActorRef[LockingResponse],
      prefix: String,
      token: String,
      currentPrefix: String
  ): LockManager = {
    val failureReason =
      s"Invalid prefix [$prefix] or token [$token] for re-acquiring the lock. Currently it is acquired by component: [$currentPrefix]"
    log.error(failureReason)
    replyTo ! ReAcquiringLockFailed(failureReason)
    new LockManager(None, this.componentName())
  }

  private def onLockReleased(prefix: String, replyTo: ActorRef[LockingResponse]): LockManager = {
    log.info(s"The lock is successfully released by component: [$prefix]")
    replyTo ! LockReleased
    new LockManager(None, this.componentName()) // TODO: Stop the timer for lock lease
  }

  private def onLockReleaseFailed(
      replyTo: ActorRef[LockingResponse],
      prefix: String,
      token: String,
      currentPrefix: String
  ): LockManager = {
    val failureReason =
      s"Invalid prefix [$prefix] or token [$token] for releasing the lock. Currently it is acquired by component: [$currentPrefix]"
    log.error(failureReason)
    replyTo ! ReleasingLockFailed(failureReason)
    this
  }

  private def onLockAlreadyReleased(prefix: String, replyTo: ActorRef[LockingResponse]): LockManager = {
    log.warn(s"Cannot release lock for [$prefix] as it is already released")
    replyTo ! LockAlreadyReleased
    this
  }

  override protected def componentName(): String = _componentName
}
