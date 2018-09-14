package csw.command.models.framework

import csw.params.TMTSerializable

/**
 * Represents protocol or messages sent to underlying TLA component
 */
sealed trait ToComponentLifecycleMessage extends TMTSerializable

object ToComponentLifecycleMessages {

  /**
   * Represents an action to go to offline mode
   */
  case object GoOffline extends ToComponentLifecycleMessage

  /**
   * Represents an action to go to online mode
   */
  case object GoOnline extends ToComponentLifecycleMessage

  /**
   * A Java helper representing GoOffline
   */
  def jGoOffline(): ToComponentLifecycleMessage = GoOffline

  /**
   * A Java helper representing GoOnline
   */
  def jGoOnline(): ToComponentLifecycleMessage = GoOnline
}
