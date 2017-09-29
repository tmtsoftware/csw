package csw.messages.framework

import csw.messages.TMTSerializable

sealed trait ContainerLifecycleState extends TMTSerializable
object ContainerLifecycleState {
  case object Idle    extends ContainerLifecycleState
  case object Running extends ContainerLifecycleState
}
