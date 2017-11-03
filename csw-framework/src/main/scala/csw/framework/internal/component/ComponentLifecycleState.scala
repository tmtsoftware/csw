package csw.framework.internal.component

/**
 * Lifecycle state of a Component TLA actor
 */
sealed trait ComponentLifecycleState
object ComponentLifecycleState {
  case object Idle        extends ComponentLifecycleState
  case object Initialized extends ComponentLifecycleState
  case object Running     extends ComponentLifecycleState
}
