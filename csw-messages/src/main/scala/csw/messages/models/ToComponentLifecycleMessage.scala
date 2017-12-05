package csw.messages.models

import csw.messages.TMTSerializable

sealed trait ToComponentLifecycleMessage extends TMTSerializable
object ToComponentLifecycleMessages {
  case object GoOffline extends ToComponentLifecycleMessage
  case object GoOnline  extends ToComponentLifecycleMessage

  def jGoOffline(): ToComponentLifecycleMessage = GoOffline
  def jGoOnline(): ToComponentLifecycleMessage  = GoOnline
}
