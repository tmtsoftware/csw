package csw.messages.models

import csw.messages.ActorTypes.ComponentRef
import csw.messages.TMTSerializable
import csw.messages.framework.ComponentInfo

case class Component(supervisor: ComponentRef, info: ComponentInfo) extends TMTSerializable
