package csw.messages.models

import csw.messages.TMTSerializable

sealed trait LockingResponse extends TMTSerializable

object LockingResponse {
  case object LockAcquired                       extends LockingResponse
  case class AcquiringLockFailed(reason: String) extends LockingResponse
  case object LockReleased                       extends LockingResponse
  case object LockAlreadyReleased                extends LockingResponse
  case class ReleasingLockFailed(reason: String) extends LockingResponse
  case object LockExpired                        extends LockingResponse
}
