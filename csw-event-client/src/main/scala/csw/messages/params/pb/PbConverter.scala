package csw.messages.params.pb

import csw.messages.events.{Event, ObserveEvent, SystemEvent}
import csw_protobuf.events.PbEvent

object PbConverter {

  def fromPbEvent[T <: Event](pbEvent: PbEvent): T = TypeMapperSupport.eventTypeMapper[T].toCustom(pbEvent)

  def toPbEvent(event: Event): PbEvent = event match {
    case se: SystemEvent  ⇒ TypeMapperSupport.eventTypeMapper[SystemEvent].toBase(se)
    case oe: ObserveEvent ⇒ TypeMapperSupport.eventTypeMapper[ObserveEvent].toBase(oe)
  }
}
