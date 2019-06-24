package csw.command.client

import akka.serialization.Serializer
import csw.command.client.messages.CommandSerializationMarker
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import csw.params.commands.CommandResponse
import csw.params.core.formats.CborSupport
import io.bullet.borer.Cbor

class CommandAkkaSerializer extends Serializer {
  import CborSupport._

  private val logger: Logger = GenericLoggerFactory.getLogger

  override def identifier: Int = 19923

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case x: CommandResponse.RemoteMsg            => Cbor.encode(x).toByteArray
      case x: CommandSerializationMarker.RemoteMsg =>
//        Cbor.encode(x).toByteArray
        throw new RuntimeException(s"does not support encoding of $o")
      case _ =>
        val ex = new RuntimeException(s"does not support encoding of $o")
        logger.error(ex.getMessage, ex = ex)
        throw ex
    }
  }

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    if (classOf[CommandResponse.RemoteMsg].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[CommandResponse.RemoteMsg].value
    } else if (classOf[CommandSerializationMarker.RemoteMsg].isAssignableFrom(manifest.get)) {
//      Cbor.decode(bytes).to[SerializationMarker.RemoteMsg].value
      throw new RuntimeException("end")
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }
}
