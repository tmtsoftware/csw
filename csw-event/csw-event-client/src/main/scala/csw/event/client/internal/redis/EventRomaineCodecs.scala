package csw.event.client.internal.redis

import java.nio.ByteBuffer

import csw.event.client.internal.commons.EventConverter
import csw.params.events.{Event, EventKey}
import romaine.codec.RomaineCodec

/**
 * Encodes and decodes keys as EventKeys and values as ProtoBuf byte equivalent of Event
 */
private[event] object EventRomaineCodecs {

  implicit val eventKeyRomaineCodec: RomaineCodec[EventKey] =
    RomaineCodec.stringCodec.bimap(_.key, EventKey.apply)

  implicit val eventRomaineCodec: RomaineCodec[Event] =
    RomaineCodec.byteBufferCodec.bimap[Event](EventConverter.toBytes[ByteBuffer], EventConverter.toEvent)

}
