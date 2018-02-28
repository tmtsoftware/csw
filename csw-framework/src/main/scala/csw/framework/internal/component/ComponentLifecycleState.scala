package csw.framework.internal.component

/**
 * Lifecycle state of a Component TLA actor
 */
//TODO: add more doc for significance like where is it used and when
sealed trait ComponentLifecycleState
object ComponentLifecycleState {
  case object Idle        extends ComponentLifecycleState
  case object Initialized extends ComponentLifecycleState
  case object Running     extends ComponentLifecycleState
}
