package csw.services.alarm.client.internal

import java.nio.ByteBuffer

import csw.services.alarm.api.models.{AlarmKey, AlarmMetadata}
import io.lettuce.core.codec.{RedisCodec, Utf8StringCodec}
import upickle.default._

/**
 * Encodes and decodes keys as AlarmKey and values as Json byte equivalent of AlarmMetadata
 */
object AlarmMetadataCodec extends RedisCodec[AlarmKey, AlarmMetadata] {

  private lazy val utf8StringCodec = new Utf8StringCodec()

  override def encodeKey(key: AlarmKey): ByteBuffer = utf8StringCodec.encodeKey(key.metadataKey)

  override def decodeKey(byteBuf: ByteBuffer): AlarmKey = AlarmKey.fromMetadataKey(utf8StringCodec.decodeKey(byteBuf))

  override def encodeValue(alarmMetadata: AlarmMetadata): ByteBuffer = utf8StringCodec.encodeValue(write(alarmMetadata))

  override def decodeValue(byteBuf: ByteBuffer): AlarmMetadata = read[AlarmMetadata](utf8StringCodec.decodeKey(byteBuf))
}
