package csw.messages.framework

import csw.messages.TMTSerializable

/**
 * Lifecycle state of a Supervisor actor
 */
//TODO: add doc for significance SupervisorLifecycleState
sealed trait SupervisorLifecycleState extends TMTSerializable
object SupervisorLifecycleState {
  case object Idle           extends SupervisorLifecycleState
  case object Running        extends SupervisorLifecycleState
  case object RunningOffline extends SupervisorLifecycleState
  case object Restart        extends SupervisorLifecycleState
  case object Shutdown       extends SupervisorLifecycleState
  case object Lock           extends SupervisorLifecycleState
}
