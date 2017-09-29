package csw.param.messages

import csw.param.TMTSerializable

sealed trait SupervisorLifecycleState extends TMTSerializable
object SupervisorLifecycleState {
  case object Idle           extends SupervisorLifecycleState
  case object Running        extends SupervisorLifecycleState
  case object RunningOffline extends SupervisorLifecycleState
  case object Restart        extends SupervisorLifecycleState
  case object Shutdown       extends SupervisorLifecycleState
}
