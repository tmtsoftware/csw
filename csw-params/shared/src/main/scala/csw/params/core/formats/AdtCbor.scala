package csw.params.core.formats

import csw.params.commands.Command
import csw.params.core.formats.CborSupport._
import csw.params.events.Event
import io.bullet.borer.{Cbor, Decoder, Encoder}

abstract class AdtCbor[T: Encoder: Decoder] {
  def encode(adt: T): Array[Byte]           = Cbor.encode(adt).toByteArray
  def decode[U <: T](bytes: Array[Byte]): U = Cbor.decode(bytes).to[T].value.asInstanceOf[U]
}

object EventCbor   extends AdtCbor[Event]
object CommandCbor extends AdtCbor[Command]
