package csw.messages.models

import akka.typed.ActorRef
import csw.messages.framework.SupervisorLifecycleState
import csw.messages.{ComponentMessage, TMTSerializable}

//TODO: what, why, how
case class LifecycleStateChanged(publisher: ActorRef[ComponentMessage], state: SupervisorLifecycleState) extends TMTSerializable
