package csw.location.server.internal

import akka.actor.ExtendedActorSystem
import csw.commons.CborAkkaSerializer
import csw.location.api.codec.{LocationCodecs, LocationSerializable}
import csw.location.api.models.{Connection, Location, Registration, TrackingEvent}

class LocationAkkaSerializer(_system: ExtendedActorSystem)
    extends CborAkkaSerializer[LocationSerializable](_system)
    with LocationCodecs {

  override val identifier: Int = 19924

  register[Connection]
  register[Location]
  register[Registration]
  register[TrackingEvent]
}
