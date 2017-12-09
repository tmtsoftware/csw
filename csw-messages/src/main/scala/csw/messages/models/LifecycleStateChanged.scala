package csw.messages.models

import csw.messages.ActorTypes.ComponentRef
import csw.messages.TMTSerializable
import csw.messages.framework.SupervisorLifecycleState

case class LifecycleStateChanged(publisher: ComponentRef, state: SupervisorLifecycleState) extends TMTSerializable
