package csw.messages.framework

import csw.messages.TMTSerializable

/**
 * Lifecycle state of a container actor
 */
//TODO: add doc what and how Idle and running state are used
sealed trait ContainerLifecycleState extends TMTSerializable
object ContainerLifecycleState {
  case object Idle    extends ContainerLifecycleState
  case object Running extends ContainerLifecycleState
}
