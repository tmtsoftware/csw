package csw.common.framework.internal.component

sealed trait ComponentLifecycleState

object ComponentLifecycleState {
  case object Idle    extends ComponentLifecycleState
  case object Running extends ComponentLifecycleState
}
