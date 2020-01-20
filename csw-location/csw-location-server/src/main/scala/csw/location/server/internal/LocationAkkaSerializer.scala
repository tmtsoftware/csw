package csw.location.server.internal

import akka.serialization.Serializer
import csw.location.api.commons.LocationServiceLogger
import csw.location.models.codecs.LocationCodecs
import csw.location.models.{Connection, Location, Registration, TrackingEvent}
import csw.logging.api.scaladsl.Logger
import io.bullet.borer.{Cbor, Decoder}

import scala.reflect.ClassTag

class LocationAkkaSerializer extends Serializer with LocationCodecs {

  override val identifier: Int = 19924
  private val logger: Logger   = LocationServiceLogger.getLogger

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
    def fromBinary[T: ClassTag: Decoder]: Option[T] = {
      val clazz = scala.reflect.classTag[T].runtimeClass
      if (clazz.isAssignableFrom(manifest.get)) Some(Cbor.decode(bytes).to[T].value)
      else None
    }

    fromBinary[Connection]
      .orElse(fromBinary[Location])
      .orElse(fromBinary[Registration])
      .orElse(fromBinary[TrackingEvent])
      .getOrElse {
        val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
        logger.error(ex.getMessage, ex = ex)
        throw ex
      }
  }
}
