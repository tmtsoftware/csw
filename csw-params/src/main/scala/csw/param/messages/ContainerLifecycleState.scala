package csw.param.messages

import csw.param.TMTSerializable

sealed trait ContainerLifecycleState extends TMTSerializable
object ContainerLifecycleState {
  case object Idle    extends ContainerLifecycleState
  case object Running extends ContainerLifecycleState
}
