package csw.event.client.internal.redis

import csw.event.client.internal.commons.EventConverter
import csw.params.events.{Event, EventKey}
import romaine.codec.RomaineByteCodec

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
private[event] object EventRomaineCodecs {

  implicit val eventKeyRomaineCodec: RomaineByteCodec[EventKey] =
    RomaineByteCodec.stringRomaineCodec.bimap(_.key, EventKey.apply)

  implicit val eventRomaineCodec: RomaineByteCodec[Event] =
    RomaineByteCodec.byteBufferRomaineCodec.bimap[Event](EventConverter.toBytes, EventConverter.toEvent)

}
