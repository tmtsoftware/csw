package csw.location.api.serializer

import csw.location.api.codec.{LocationCodecs, LocationSerializable}
import csw.location.api.models.{Connection, Location, Registration, TrackingEvent}
import csw.commons.CborAkkaSerializer

class LocationAkkaSerializer extends CborAkkaSerializer[LocationSerializable] with LocationCodecs {

  override val identifier: Int = 19924

  register[Connection]
  register[Location]
  register[Registration]
  register[TrackingEvent]
}
