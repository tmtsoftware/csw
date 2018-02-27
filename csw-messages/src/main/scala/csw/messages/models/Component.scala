package csw.messages.models

import akka.typed.ActorRef
import csw.messages.framework.ComponentInfo
import csw.messages.{ComponentMessage, TMTSerializable}

//TODO: what, why, how
case class Component(supervisor: ActorRef[ComponentMessage], info: ComponentInfo) extends TMTSerializable
