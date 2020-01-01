package csw.command.api.codecs

import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs._
import msocket.api
import msocket.api.ErrorProtocol
import msocket.api.codecs.BasicCodecs
import msocket.api.models.ServiceError

object CommandServiceCodecs extends CommandServiceCodecs

trait CommandServiceCodecs extends ParamCodecs with BasicCodecs {
  implicit lazy val httpCodecsValue: Codec[CommandServiceHttpMessage]      = deriveAllCodecs
  implicit lazy val websocketCodecs: Codec[CommandServiceWebsocketMessage] = deriveAllCodecs

  implicit lazy val CommandServiceHttpErrorProtocol: ErrorProtocol[CommandServiceHttpMessage] =
    ErrorProtocol.bind[CommandServiceHttpMessage, ServiceError]

  implicit lazy val CommandServiceWebsocketErrorProtocol: api.ErrorProtocol[CommandServiceWebsocketMessage] =
    ErrorProtocol.bind[CommandServiceWebsocketMessage, ServiceError]

}
