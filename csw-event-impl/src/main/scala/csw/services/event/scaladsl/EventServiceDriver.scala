package csw.services.event.scaladsl

import csw_protobuf.events.PbEvent

trait EventServiceDriver {

  def publishToChannel(data: PbEvent, channel: String)

}
