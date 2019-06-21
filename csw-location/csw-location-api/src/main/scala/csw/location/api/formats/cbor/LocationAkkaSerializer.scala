package csw.location.api.formats.cbor

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.serialization.Serializer
import csw.location.api.models.{Connection, Location, Registration, TrackingEvent}
import io.bullet.borer.Cbor

class LocationAkkaSerializer(_actorSystem: ExtendedActorSystem) extends Serializer with LocationCborSupport {

  override implicit lazy val actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 19924

  override def toBinary(o: AnyRef): Array[Byte] = {
    try {
      o match {
        case x: Connection    => Cbor.encode(x).toByteArray
        case x: Location      => Cbor.encode(x).toByteArray
        case x: Registration  => Cbor.encode(x).toByteArray
        case x: TrackingEvent => Cbor.encode(x).toByteArray
        case _                => throw new RuntimeException(s"does not support encoding of $o")
      }
    } catch {
      case ex => ex.printStackTrace(); throw ex
    }
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    try {
      if (classOf[Connection].isAssignableFrom(manifest.get)) {
        Cbor.decode(bytes).to[Connection].value
      } else if (classOf[Location].isAssignableFrom(manifest.get)) {
        Cbor.decode(bytes).to[Location].value
      } else if (classOf[Registration].isAssignableFrom(manifest.get)) {
        Cbor.decode(bytes).to[Registration].value
      } else if (classOf[TrackingEvent].isAssignableFrom(manifest.get)) {
        Cbor.decode(bytes).to[TrackingEvent].value
      } else {
        throw new RuntimeException("end")
      }

    } catch {
      case ex => ex.printStackTrace(); throw ex

    }

  }
}
