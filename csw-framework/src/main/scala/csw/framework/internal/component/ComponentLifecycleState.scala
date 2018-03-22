package csw.framework.internal.component

/**
 * Lifecycle state of a Component TLA actor
 */
private[framework] sealed trait ComponentLifecycleState
private[framework] object ComponentLifecycleState {
  case object Idle        extends ComponentLifecycleState
  case object Initialized extends ComponentLifecycleState
  case object Running     extends ComponentLifecycleState
}
