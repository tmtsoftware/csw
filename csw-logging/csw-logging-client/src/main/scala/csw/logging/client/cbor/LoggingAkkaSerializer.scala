package csw.logging.client.cbor

import akka.serialization.Serializer
import csw.logging.api.models.Level
import csw.logging.api.scaladsl.Logger
import csw.logging.client.cbor.LoggingCodecs._
import csw.logging.client.models.LogMetadata
import csw.logging.client.scaladsl.GenericLoggerFactory
import io.bullet.borer.Cbor

class LoggingAkkaSerializer extends Serializer {

  private val logger: Logger   = GenericLoggerFactory.getLogger
  override def identifier: Int = 19925

  override def toBinary(o: AnyRef): Array[Byte] = {
    println("*" * 80)
    println(o)
    o match {
      case x: LogMetadata => Cbor.encode(x).toByteArray
      case x: Level       => Cbor.encode(x).toByteArray
      case _ =>
        val ex = new RuntimeException(s"does not support encoding of $o")
        logger.error(ex.getMessage, ex = ex)
        throw ex
    }
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    println(manifest.get)
    if (classOf[LogMetadata].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[LogMetadata].value
    } else if (classOf[Level].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[Level].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }
}
