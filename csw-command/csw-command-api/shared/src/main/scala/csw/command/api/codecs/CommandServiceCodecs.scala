package csw.command.api.codecs

import csw.command.api.messages.{CommandServiceRequest, CommandServiceStreamRequest}
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api
import msocket.api.ErrorProtocol
import msocket.api.codecs.BasicCodecs
import msocket.api.models.ServiceError

object CommandServiceCodecs extends CommandServiceCodecs

trait CommandServiceCodecs extends ParamCodecs with BasicCodecs {
  implicit lazy val httpCodecsValue: Codec[CommandServiceRequest]       = deriveAllCodecs
  implicit lazy val websocketCodecs: Codec[CommandServiceStreamRequest] = deriveAllCodecs

  implicit lazy val CommandServiceHttpErrorProtocol: ErrorProtocol[CommandServiceRequest] =
    ErrorProtocol.bind[CommandServiceRequest, ServiceError]

  implicit lazy val CommandServiceWebsocketErrorProtocol: api.ErrorProtocol[CommandServiceStreamRequest] =
    ErrorProtocol.bind[CommandServiceStreamRequest, ServiceError]

}
