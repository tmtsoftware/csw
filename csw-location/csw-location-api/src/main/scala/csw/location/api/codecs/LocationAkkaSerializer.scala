package csw.location.api.codecs

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.serialization.Serializer
import csw.location.api.commons.LocationServiceLogger
import csw.location.api.models.{Connection, Location, Registration, TrackingEvent}
import csw.logging.api.scaladsl.Logger
import io.bullet.borer.Cbor

class LocationAkkaSerializer(_actorSystem: ExtendedActorSystem) extends Serializer with LocationCodecs {

  override val identifier: Int                      = 19924
  private val logger: Logger                        = LocationServiceLogger.getLogger
  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def includeManifest: Boolean = true

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: Connection    => Cbor.encode(x).toByteArray
    case x: Location      => Cbor.encode(x).toByteArray
    case x: Registration  => Cbor.encode(x).toByteArray
    case x: TrackingEvent => Cbor.encode(x).toByteArray
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    if (classOf[Connection].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Connection].value
    } else if (classOf[Location].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Location].value
    } else if (classOf[Registration].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Registration].value
    } else if (classOf[TrackingEvent].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[TrackingEvent].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }

}
