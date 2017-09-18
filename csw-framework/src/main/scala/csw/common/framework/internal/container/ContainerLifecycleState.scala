package csw.common.framework.internal.container

sealed trait ContainerLifecycleState
object ContainerLifecycleState {
  case object Idle    extends ContainerLifecycleState
  case object Running extends ContainerLifecycleState
}
