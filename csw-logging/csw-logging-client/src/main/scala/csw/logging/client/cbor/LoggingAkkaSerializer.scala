package csw.logging.client.cbor

import akka.serialization.Serializer
import csw.logging.api.scaladsl.Logger
import csw.logging.models.codecs.LoggingCodecs._
import csw.logging.client.scaladsl.GenericLoggerFactory
import csw.logging.models.{Level, LogMetadata}
import io.bullet.borer.{Cbor, Decoder}

import scala.reflect.ClassTag

class LoggingAkkaSerializer extends Serializer {
  private val logger: Logger   = GenericLoggerFactory.getLogger
  override def identifier: Int = 19925

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case x: LogMetadata => Cbor.encode(x).toByteArray
    case x: Level       => Cbor.encode(x).toByteArray
    case _ =>
      val ex = new RuntimeException(s"does not support encoding of $o")
      logger.error(ex.getMessage, ex = ex)
      throw ex
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    def fromBinary[T: ClassTag: Decoder]: Option[T] = {
      val clazz = scala.reflect.classTag[T].runtimeClass
      if (clazz.isAssignableFrom(manifest.get)) Some(Cbor.decode(bytes).to[T].value)
      else None
    }

    fromBinary[LogMetadata]
      .orElse(fromBinary[Level])
      .getOrElse {
        val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
        logger.error(ex.getMessage, ex = ex)
        throw ex
      }
  }
}
