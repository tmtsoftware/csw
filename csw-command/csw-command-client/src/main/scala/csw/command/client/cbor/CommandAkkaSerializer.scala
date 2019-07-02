package csw.command.client.cbor

import akka.actor.ExtendedActorSystem
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.serialization.Serializer
import csw.command.client.messages.CommandSerializationMarker.RemoteMsg
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import csw.params.commands.CommandResponse
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.Cbor

class CommandAkkaSerializer(_actorSystem: ExtendedActorSystem) extends Serializer with MessageCodecs with ParamCodecs {

  private val logger: Logger                        = GenericLoggerFactory.getLogger
  override implicit def actorSystem: ActorSystem[_] = _actorSystem.toTyped

  override def identifier: Int = 19923

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case x: CommandResponse.RemoteMsg => Cbor.encode(x).toByteArray
//      case x: StateVariable             => Cbor.encode(x).toByteArray
      case x: RemoteMsg => Cbor.encode(x).toByteArray
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
//    } else if (classOf[StateVariable].isAssignableFrom(manifest.get)) {
//      Cbor.decode(bytes).to[StateVariable].value
    } else if (classOf[RemoteMsg].isAssignableFrom(manifest.get)) {
      Cbor.decode(bytes).to[RemoteMsg].value
    } else {
      val ex = new RuntimeException(s"does not support decoding of ${manifest.get}")
      logger.error(ex.getMessage, ex = ex)
      throw ex
    }
  }
}
