package csw.framework.internal.supervisor

import akka.Done
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.pattern.StatusReply
import csw.command.client.models.framework.LockingResponse
import csw.command.client.models.framework.LockingResponse._
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.FiniteDuration

object LockManager2 {
  val AdminPrefix = Prefix(s"${Subsystem.CSW}.admin")

  private val LockNotificationKey = "lockNotification"
  private val LockExpirationKey   = "lockExpiration"

  trait LockManager2Message

  final case class LockComponent(
      source: Prefix,
      replyTo: ActorRef[LockingResponse],
      svrReplyTo: ActorRef[LockManager2Response],
      leaseDuration: FiniteDuration
  ) extends LockManager2Message

  final case class UnlockComponent(source: Prefix, replyTo: ActorRef[LockingResponse], svrReplyTo: ActorRef[LockManager2Response])
      extends LockManager2Message

  final case class IsLocked(replyTo: ActorRef[LockManager2Response]) extends LockManager2Message

  final case class LockPrefix(replyTo: ActorRef[LockManager2Response]) extends LockManager2Message

  final case class IsCommandPrefixAllowed(cmdPrefix: Prefix, replyTo: ActorRef[StatusReply[Done]]) extends LockManager2Message

  private final case class LockTimedout(replyTo: ActorRef[LockingResponse]) extends LockManager2Message

  private final case class LockAboutToTimeout(replyTo: ActorRef[LockingResponse]) extends LockManager2Message

  trait LockManager2Response

  final case object Locked extends LockManager2Response

  final case object Unlocked extends LockManager2Response

  final case object CommandPrefixAllowed extends LockManager2Response

  final case object CommandPrefixNotAllowed extends LockManager2Response

  final case object LockReleased2 extends LockManager2Response

  final case class LockReleaseFailed(sourcePrefix: Prefix, currentPrefix: Prefix) extends LockManager2Response

  final case class AcquiringLockFailed2(prefix: Prefix, currentPrefix: Prefix) extends LockManager2Response

  final case class LockPrefixResponse(prefix: Prefix) extends LockManager2Response

  final case object Unhandled extends LockManager2Response

  def apply(loggerFactory: LoggerFactory): Behavior[LockManager2Message] = {
    println("Yes LockManager2")
    unlocked(loggerFactory)
  }

  def unlocked(loggerFactory: LoggerFactory): Behavior[LockManager2Message] = {
    Behaviors.withTimers { timerScheduler =>
      Behaviors.receiveMessage {
        case LockComponent(lockPrefix, replyTo, svrReplyTo, leaseDuration) =>
          println(s"Locking for: $lockPrefix and $replyTo")
          //log.info(s"The lock is successfully acquired by component: $source")
          timerScheduler.startSingleTimer(LockNotificationKey, LockAboutToTimeout(replyTo), leaseDuration - (leaseDuration / 10))
          timerScheduler.startSingleTimer(LockExpirationKey, LockTimedout(replyTo), leaseDuration)
          // Send client LockAcquired
          replyTo ! LockAcquired
          println(s"Sent lockAcquired to replyTo: $replyTo")
          locked(svrReplyTo, timerScheduler, lockPrefix, loggerFactory)
        case UnlockComponent(_, replyTo, _) =>
          // This shouldn't happen since super will be in locked state to get Unlocked, super refuses UnlockComponent
          replyTo ! LockReleased
          Behaviors.same
        case IsLocked(replyTo) =>
          replyTo ! Unlocked
          Behaviors.same
        case LockPrefix(svrReplyTo) =>
          svrReplyTo ! Unhandled
          Behaviors.unhandled
      }
    }
  }

  def locked(
      svrReplyTo: ActorRef[LockManager2Response],
      timerScheduler: TimerScheduler[LockManager2Message],
      lockPrefix: Prefix,
      loggerFactory: LoggerFactory
  ): Behavior[LockManager2Message] = {
    Behaviors
      .receiveMessage[LockManager2Message] {
        case IsLocked(replyTo) =>
          replyTo ! Locked
          Behaviors.same
        case LockAboutToTimeout(replyTo) =>
          replyTo ! LockExpiringShortly
          Behaviors.same
        case LockTimedout(replyTo) =>
          replyTo ! LockExpired
          Behaviors.stopped
        case LockComponent(source, replyTo, _, _) if source != lockPrefix =>
          val failureReason =
            s"Invalid source ${source} for acquiring lock. Lock is currently owned by component: ${lockPrefix}."
          //log.error(failureReason)
          println(failureReason)
          // Client receives AcquireLockFailed
          replyTo ! AcquiringLockFailed(failureReason)
          Behaviors.same
        case LockComponent(_, replyTo, svrReplyTo, leaseDuration) =>
          // Reacquire lock with current prefix
          timerScheduler.startSingleTimer(LockNotificationKey, LockAboutToTimeout(replyTo), leaseDuration - (leaseDuration / 10))
          timerScheduler.startSingleTimer(LockExpirationKey, LockTimedout(replyTo), leaseDuration)
          // Send client LockAcquired
          replyTo ! LockAcquired
          locked(svrReplyTo, timerScheduler, lockPrefix, loggerFactory)
        case UnlockComponent(unlockPrefix, replyTo, svrReplyTo) =>
          if (unlockPrefix == lockPrefix || unlockPrefix == AdminPrefix) {
            println(s"The lock is successfully released by component: ${unlockPrefix}")
            replyTo ! LockReleased
            timerScheduler.cancel(LockNotificationKey)
            timerScheduler.cancel(LockExpirationKey)
            svrReplyTo ! LockReleased2
            Behaviors.stopped
          }
          else {
            println("Unlock failure")
            val failureReason =
              s"Invalid prefix ${unlockPrefix} for releasing lock. Currently the component is locked by: ${lockPrefix}."
            //log.error(failureReason)
            println(failureReason)
            replyTo ! ReleasingLockFailed(failureReason)
            //svrReplyTo ! AcquiringLockFailed2(unlockPrefix, lockPrefix)
            Behaviors.same
          }
        case IsCommandPrefixAllowed(unlockPrefix, replyTo) =>
          if (unlockPrefix == lockPrefix || unlockPrefix == AdminPrefix) {
            replyTo ! StatusReply.Ack
          }
          else {
            replyTo ! StatusReply.Error(s"Prefix: $unlockPrefix is not allowed.")
          }
          Behaviors.same
        case LockPrefix(replyTo) =>
          replyTo ! LockPrefixResponse(lockPrefix)
          Behaviors.stopped
      }
      .receiveSignal {
        case (context: ActorContext[LockManager2Message], PostStop) =>
          println(s"PostStop signal received for lockManager: $lockPrefix.")
          Behaviors.same
      }
  }

}
