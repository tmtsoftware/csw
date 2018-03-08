package csw.messages.framework

import akka.typed.ActorRef
import csw.messages.{ComponentMessage, TMTSerializable}

//TODO: what, why, how
case class Component(supervisor: ActorRef[ComponentMessage], info: ComponentInfo) extends TMTSerializable
