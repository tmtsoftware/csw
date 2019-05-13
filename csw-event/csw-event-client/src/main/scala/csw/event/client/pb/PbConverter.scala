package csw.event.client.pb

import csw.params.events.Event
import csw_protobuf.events.PbEvent

object PbConverter {
  def fromPbEvent[T <: Event](pbEvent: PbEvent): T = TypeMapperSupport.eventTypeMapper[T].toCustom(pbEvent)
  def toPbEvent(event: Event): PbEvent             = TypeMapperSupport.eventTypeMapper.toBase(event)
}
