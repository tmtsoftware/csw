package csw.messages.models

import csw.messages.TMTSerializable

//TODO: what, why, how
sealed trait LockingResponse extends TMTSerializable

object LockingResponses {

  /**
   * Scala API: Lock successfully acquired acknowledgment
   */
  case object LockAcquired extends LockingResponse

  /**
   * Java API: Lock successfully acquired acknowledgment
   */
  def lockAcquired: LockingResponse = LockAcquired

  /**
   * This is sent when lock acquiring fails, e.g. Component A tries to Lock component B which is already locked by Component C
   */
  case class AcquiringLockFailed(reason: String) extends LockingResponse

  /**
   * Scala API: Lock successfully released acknowledgment
   */
  case object LockReleased extends LockingResponse

  /**
   * Java API: Lock successfully released acknowledgment
   */
  def lockReleased: LockingResponse = LockReleased

  /**
   * Scala API: Lock already released acknowledgment, this is sent when unlocked component receives `Unlock` message
   */
  case object LockAlreadyReleased extends LockingResponse

  /**
   * Java API: Lock already released acknowledgment, this is sent when unlocked component receives `Unlock` message
   */
  def lockAlreadyReleased: LockingResponse = LockAlreadyReleased

  /**
   * This is sent when unlocking component fails, e.g. Component A tries to Unlock component B which is locked by Component C
   */
  case class ReleasingLockFailed(reason: String) extends LockingResponse

  /**
   * Scala API: Lock expired notification which is sent to component who acquired lock previously
   */
  case object LockExpired extends LockingResponse

  /**
   * Java API: Lock expired notification which is sent to component who acquired lock previously
   */
  def lockExpired: LockingResponse = LockAcquired

  /**
   * Scala API: Lock expiring notification which is sent after a duration calculated by [leaseDuration - (leaseDuration / 10)]
   */
  case object LockExpiringShortly extends LockingResponse

  /**
   * Java API: Lock expiring notification which is sent after a duration calculated by [leaseDuration - (leaseDuration / 10)]
   */
  def lockExpiringShortly: LockingResponse = LockExpiringShortly
}
