package csw.common.framework.internal.supervisor

sealed trait SupervisorMode

object SupervisorMode {
  case object Idle           extends SupervisorMode
  case object Running        extends SupervisorMode
  case object RunningOffline extends SupervisorMode
}
