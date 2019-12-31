package csw.command.api.codecs

import com.github.ghik.silencer.silent
import csw.command.api.messages.{CommandServiceHttpMessage, CommandServiceWebsocketMessage}
import csw.command.api.messages.CommandServiceHttpMessage._
import csw.command.api.messages.CommandServiceWebsocketMessage._
import csw.params.core.formats.ParamCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import msocket.api
import msocket.api.ErrorProtocol
import msocket.api.codecs.BasicCodecs
import msocket.api.models.ServiceError

object CommandServiceCodecs extends CommandServiceCodecs

trait CommandServiceCodecs extends ParamCodecs with BasicCodecs {

  implicit def httpCodec[T <: CommandServiceHttpMessage]: Codec[T] = httpCodecsValue.asInstanceOf[Codec[T]]

  lazy val httpCodecsValue: Codec[CommandServiceHttpMessage] = {
    @silent implicit lazy val validateCodec: Codec[Validate] = deriveUnaryCodec
    @silent implicit lazy val submitCodec: Codec[Submit]     = deriveUnaryCodec
    @silent implicit lazy val onewayCodec: Codec[Oneway]     = deriveUnaryCodec
    @silent implicit lazy val queryCodec: Codec[Query]       = deriveUnaryCodec
    deriveCodec
  }

  implicit def websocketCodec[T <: CommandServiceWebsocketMessage]: Codec[T] = websocketCodecs.asInstanceOf[Codec[T]]

  lazy val websocketCodecs: Codec[CommandServiceWebsocketMessage] = {
    @silent implicit lazy val queryFinalCodec: Codec[QueryFinal]                       = deriveCodec
    @silent implicit lazy val subscribeCurrentStateCodec: Codec[SubscribeCurrentState] = deriveUnaryCodec
    deriveCodec
  }

  implicit lazy val CommandServiceHttpErrorProtocol: ErrorProtocol[CommandServiceHttpMessage] =
    ErrorProtocol.bind[CommandServiceHttpMessage, ServiceError]

  implicit lazy val CommandServiceWebsocketErrorProtocol: api.ErrorProtocol[CommandServiceWebsocketMessage] =
    ErrorProtocol.bind[CommandServiceWebsocketMessage, ServiceError]

}
