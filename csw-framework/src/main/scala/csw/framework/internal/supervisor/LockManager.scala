package csw.framework.internal.supervisor

import akka.typed.ActorRef
import csw.messages.CommandMessage
import csw.messages.ccs.CommandIssue.ComponentLockedIssue
import csw.messages.ccs.commands.CommandResponse.NotAllowed
import csw.messages.models.LockingResponse
import csw.messages.models.LockingResponse._
import csw.messages.params.models.Prefix
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

class LockManager(val lockPrefix: Option[Prefix], loggerFactory: LoggerFactory) {
  val log: Logger = loggerFactory.getLogger

  def lockComponent(prefix: Prefix, replyTo: ActorRef[LockingResponse])(startTimer: ⇒ Unit): LockManager = lockPrefix match {
    case None                ⇒ onAcquiringLock(prefix, replyTo, startTimer)
    case Some(`prefix`)      ⇒ onReAcquiringLock(prefix, replyTo, startTimer)
    case Some(currentPrefix) ⇒ onAcquiringFailed(replyTo, prefix, currentPrefix)
  }

  def unlockComponent(prefix: Prefix, replyTo: ActorRef[LockingResponse])(stopTimer: ⇒ Unit): LockManager = lockPrefix match {
    case Some(`prefix`)      ⇒ onLockReleased(prefix, replyTo, stopTimer)
    case Some(currentPrefix) ⇒ onLockReleaseFailed(replyTo, prefix, currentPrefix)
    case None                ⇒ onLockAlreadyReleased(prefix, replyTo)
  }

  def allowCommand(msg: CommandMessage): Boolean = lockPrefix match {
    case None ⇒ true
    case Some(currentPrefix) ⇒
      msg.command.source match {
        case `currentPrefix` ⇒
          log.info(s"Forwarding message ${msg.toString} to TLA for component: $currentPrefix")
          true
        case _ ⇒
          log.error(s"Cannot process the command [${msg.command.toString}] as the lock is acquired by component: $currentPrefix")
          msg.replyTo ! NotAllowed(msg.command.runId,
                                   ComponentLockedIssue(s"This component is locked by component $currentPrefix"))
          false
      }
  }

  def isLocked: Boolean = lockPrefix.isDefined

  def isUnLocked: Boolean = lockPrefix.isEmpty

  private def onAcquiringLock(prefix: Prefix, replyTo: ActorRef[LockingResponse], startTimer: ⇒ Unit): LockManager = {
    log.info(s"The lock is successfully acquired by component: $prefix")
    replyTo ! LockAcquired
    startTimer
    new LockManager(Some(prefix), loggerFactory)
  }

  private def onReAcquiringLock(prefix: Prefix, replyTo: ActorRef[LockingResponse], startTimer: ⇒ Unit): LockManager = {
    log.info(s"The lock is re-acquired by component: $prefix")
    replyTo ! LockAcquired
    startTimer
    this
  }

  private def onAcquiringFailed(replyTo: ActorRef[LockingResponse], prefix: Prefix, currentPrefix: Prefix) = {
    val failureReason = s"Invalid prefix $prefix for acquiring lock. Currently it is acquired by component: $currentPrefix"
    log.error(failureReason)
    replyTo ! AcquiringLockFailed(failureReason)
    this
  }

  private def onLockReleased(prefix: Prefix, replyTo: ActorRef[LockingResponse], stopTimer: ⇒ Unit): LockManager = {
    log.info(s"The lock is successfully released by component: $prefix")
    replyTo ! LockReleased
    stopTimer
    new LockManager(None, loggerFactory)
  }

  private def onLockReleaseFailed(replyTo: ActorRef[LockingResponse], prefix: Prefix, currentPrefix: Prefix) = {
    val failureReason = s"Invalid prefix $prefix for releasing lock. Currently it is acquired by component: $currentPrefix"
    log.error(failureReason)
    replyTo ! ReleasingLockFailed(failureReason)
    this
  }

  private def onLockAlreadyReleased(prefix: Prefix, replyTo: ActorRef[LockingResponse]): LockManager = {
    log.warn(s"Cannot release lock for $prefix as it is already released")
    replyTo ! LockAlreadyReleased
    this
  }
}
