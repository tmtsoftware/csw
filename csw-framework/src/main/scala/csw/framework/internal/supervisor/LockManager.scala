package csw.framework.internal.supervisor

import akka.actor.typed.ActorRef
import csw.messages.commands.CommandIssue.ComponentLockedIssue
import csw.messages.commands.CommandResponse.NotAllowed
import csw.messages.framework.LockingResponse
import csw.messages.framework.LockingResponses._
import csw.messages.params.models.Prefix
import csw.messages.scaladsl.CommandMessage
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

private[framework] class LockManager(val lockPrefix: Option[Prefix], loggerFactory: LoggerFactory) {
  private val log: Logger = loggerFactory.getLogger

  def lockComponent(source: Prefix, replyTo: ActorRef[LockingResponse])(startTimer: ⇒ Unit): LockManager = lockPrefix match {
    case None                ⇒ onAcquiringLock(source, replyTo, startTimer)
    case Some(`source`)      ⇒ onReAcquiringLock(source, replyTo, startTimer)
    case Some(currentPrefix) ⇒ onAcquiringFailed(replyTo, source, currentPrefix)
  }

  def unlockComponent(source: Prefix, replyTo: ActorRef[LockingResponse])(stopTimer: ⇒ Unit): LockManager = lockPrefix match {
    case Some(`source`)      ⇒ onLockReleased(source, replyTo, stopTimer)
    case Some(currentPrefix) ⇒ onLockReleaseFailed(replyTo, source, currentPrefix)
    case None                ⇒ onLockAlreadyReleased(source, replyTo)
  }

  def releaseLockOnTimeout(): LockManager = new LockManager(None, loggerFactory)

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

  private def onAcquiringLock(source: Prefix, replyTo: ActorRef[LockingResponse], startTimer: ⇒ Unit): LockManager = {
    log.info(s"The lock is successfully acquired by component: $source")
    replyTo ! LockAcquired
    startTimer
    new LockManager(Some(source), loggerFactory)
  }

  private def onReAcquiringLock(source: Prefix, replyTo: ActorRef[LockingResponse], startTimer: ⇒ Unit): LockManager = {
    log.info(s"The lock is re-acquired by component: $source")
    replyTo ! LockAcquired
    startTimer
    this
  }

  private def onAcquiringFailed(replyTo: ActorRef[LockingResponse], source: Prefix, currentPrefix: Prefix) = {
    val failureReason =
      s"Invalid source ${source.prefix} for acquiring lock. Currently it is acquired by component: ${currentPrefix.prefix}"
    log.error(failureReason)
    replyTo ! AcquiringLockFailed(failureReason)
    this
  }

  private def onLockReleased(source: Prefix, replyTo: ActorRef[LockingResponse], stopTimer: ⇒ Unit): LockManager = {
    log.info(s"The lock is successfully released by component: ${source.prefix}")
    replyTo ! LockReleased
    stopTimer
    new LockManager(None, loggerFactory)
  }

  private def onLockReleaseFailed(replyTo: ActorRef[LockingResponse], source: Prefix, currentPrefix: Prefix) = {
    val failureReason =
      s"Invalid source ${source.prefix} for releasing lock. Currently it is acquired by component: ${currentPrefix.prefix}"
    log.error(failureReason)
    replyTo ! ReleasingLockFailed(failureReason)
    this
  }

  private def onLockAlreadyReleased(source: Prefix, replyTo: ActorRef[LockingResponse]): LockManager = {
    log.warn(s"Cannot release lock for ${source.prefix} as it is already released")
    replyTo ! LockAlreadyReleased
    this
  }
}
