package csw.messages.framework

import csw.messages.TMTSerializable

//TODO: what, why, how
sealed trait ToComponentLifecycleMessage extends TMTSerializable
object ToComponentLifecycleMessages {
  case object GoOffline extends ToComponentLifecycleMessage
  case object GoOnline  extends ToComponentLifecycleMessage

  def jGoOffline(): ToComponentLifecycleMessage = GoOffline
  def jGoOnline(): ToComponentLifecycleMessage  = GoOnline
}
