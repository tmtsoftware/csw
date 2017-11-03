package csw.framework.internal.component

sealed trait ComponentLifecycleState

/**
 * Lifecycle state of a Component TLA actor
 */
object ComponentLifecycleState {
  case object Idle        extends ComponentLifecycleState
  case object Initialized extends ComponentLifecycleState
  case object Running     extends ComponentLifecycleState
}
