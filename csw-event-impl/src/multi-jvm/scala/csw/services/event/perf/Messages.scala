package csw.services.event.perf

import akka.actor.ActorRef
import csw.messages.events.{Event, EventKey, EventName, SystemEvent}
import csw.messages.params.models.{Id, Prefix}

object Messages {

  case object Run
  // Fixme: dolly, pls delete me
  sealed trait Echo
  final case class Start(correspondingReceiver: ActorRef) extends Echo
  final case object End                                   extends Echo
  final case class Warmup(msg: AnyRef)
  final case class EndResult(totalReceived: Long)
  final case class FlowControl(id: Int, burstStartTime: Long) extends Echo

}

object Helpers {
  val prefix          = Prefix("tcs.mobie.blue")
  val eventName       = EventName("filter")
  val warmupEventName = EventName("warmup")

  val eventKeys: Set[EventKey] = Set(EventKey(s"$prefix.$eventName"))

  def warmupEvent: Event = SystemEvent(prefix, warmupEventName)

  def makeEvent(id: Long): Event = SystemEvent(prefix, eventName).copy(eventId = Id(id.toString))

}
