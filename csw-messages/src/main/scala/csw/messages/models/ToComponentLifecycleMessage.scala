package csw.messages.models

import csw.messages.TMTSerializable

sealed trait ToComponentLifecycleMessage extends TMTSerializable

object ToComponentLifecycleMessage {
  case object GoOffline extends ToComponentLifecycleMessage
  case object GoOnline  extends ToComponentLifecycleMessage
}
