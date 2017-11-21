package csw.messages.models

import akka.typed.ActorRef
import csw.messages.framework.SupervisorLifecycleState
import csw.messages.{SupervisorExternalMessage, TMTSerializable}

case class LifecycleStateChanged(publisher: ActorRef[SupervisorExternalMessage], state: SupervisorLifecycleState)
    extends TMTSerializable
