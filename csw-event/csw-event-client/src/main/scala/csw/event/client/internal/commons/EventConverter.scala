package csw.event.client.internal.commons

import csw.params.events.Event
import io.bullet.borer.Cbor.DecodingConfig
import io.bullet.borer.{Cbor, Input, Output}

import scala.util.control.NonFatal

private[event] object EventConverter {
  import csw.params.core.formats.CborSupport._

  def toEvent[Chunk: Input.Wrapper](bytes: Chunk): Event = {
    try {
      Cbor.decode(bytes).withConfig(DecodingConfig(readDoubleAlsoAsFloat = true)).to[Event].value
    } catch {
      case NonFatal(_) ⇒ Event.badEvent()
    }
  }

  def toBytes[Chunk: Output.Provider](event: Event): Chunk = {
    Cbor.encode(event).to[Chunk].bytes
  }
}
