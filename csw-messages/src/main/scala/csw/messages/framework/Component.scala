package csw.messages.framework

import akka.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.scaladsl.ComponentMessage

//TODO: what, why, how
case class Component(supervisor: ActorRef[ComponentMessage], info: ComponentInfo) extends TMTSerializable
