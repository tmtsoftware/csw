package csw.messages.models

import akka.typed.ActorRef
import csw.messages.framework.SupervisorLifecycleState
import csw.messages.{ComponentMessage, TMTSerializable}

case class LifecycleStateChanged(publisher: ActorRef[ComponentMessage], state: SupervisorLifecycleState) extends TMTSerializable
