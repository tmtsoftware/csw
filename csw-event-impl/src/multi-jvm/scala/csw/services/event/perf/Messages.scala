package csw.services.event.perf

import akka.actor.ActorRef
import csw.messages.TMTSerializable

object Messages {

  case class Init(correspondingReceiver: ActorRef)            extends TMTSerializable
  case object Initialized                                     extends TMTSerializable
  final case object Start                                     extends TMTSerializable
  final case class EndResult(totalReceived: Long)             extends TMTSerializable
  final case class FlowControl(id: Int, burstStartTime: Long) extends TMTSerializable

}
