package csw.messages.models

import akka.typed.ActorRef
import csw.messages.framework.ComponentInfo
import csw.messages.{SupervisorExternalMessage, TMTSerializable}

case class Component(supervisor: ActorRef[SupervisorExternalMessage], info: ComponentInfo) extends TMTSerializable
