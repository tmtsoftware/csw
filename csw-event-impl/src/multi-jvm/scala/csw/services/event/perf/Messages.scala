package csw.services.event.perf

import akka.actor.ActorRef
import akka.testkit.JavaSerializable

object Messages {

  case class Init(correspondingReceiver: ActorRef)            extends JavaSerializable
  case object Initialized                                     extends JavaSerializable
  final case object Start                                     extends JavaSerializable
  final case class EndResult(totalReceived: Long)             extends JavaSerializable
  final case class FlowControl(id: Int, burstStartTime: Long) extends JavaSerializable

}
