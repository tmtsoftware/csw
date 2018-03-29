package csw.services.event.perf

import akka.actor.{ActorRef, DeadLetterSuppression}
import akka.testkit.JavaSerializable

object Messages {

  case object Run
  case class Init(correspondingReceiver: ActorRef)            extends Echo
  case object Initialized                                     extends Echo
  sealed trait Echo                                           extends DeadLetterSuppression with JavaSerializable
  final case class Start(correspondingReceiver: ActorRef)     extends Echo
  final case object End                                       extends Echo
  final case class EndResult(totalReceived: Long)             extends JavaSerializable
  final case class FlowControl(id: Int, burstStartTime: Long) extends Echo

}
