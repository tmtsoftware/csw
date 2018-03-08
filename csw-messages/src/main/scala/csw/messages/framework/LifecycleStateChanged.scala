package csw.messages.framework

import akka.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.scaladsl.ComponentMessage

//TODO: what, why, how
case class LifecycleStateChanged(publisher: ActorRef[ComponentMessage], state: SupervisorLifecycleState) extends TMTSerializable
