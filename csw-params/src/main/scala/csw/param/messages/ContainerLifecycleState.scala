package csw.param.messages

import csw.param.ParamSerializable

sealed trait ContainerLifecycleState extends ParamSerializable
object ContainerLifecycleState {
  case object Idle    extends ContainerLifecycleState
  case object Running extends ContainerLifecycleState
}
