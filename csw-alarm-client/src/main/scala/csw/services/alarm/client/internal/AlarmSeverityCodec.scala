package csw.services.alarm.client.internal

import java.nio.ByteBuffer

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}
import io.lettuce.core.codec.{RedisCodec, Utf8StringCodec}
import upickle.default._

/**
 * Encodes and decodes keys as AlarmKey and values as Json byte equivalent of AlarmSeverity
 */
object AlarmSeverityCodec extends RedisCodec[AlarmKey, AlarmSeverity] {

  private lazy val utf8StringCodec = new Utf8StringCodec()

  override def encodeKey(key: AlarmKey): ByteBuffer = utf8StringCodec.encodeKey(key.severityKey)

  override def decodeKey(byteBuf: ByteBuffer): AlarmKey = AlarmKey.fromSeverityKey(utf8StringCodec.decodeKey(byteBuf))

  override def encodeValue(alarmSeverity: AlarmSeverity): ByteBuffer = utf8StringCodec.encodeValue(write(alarmSeverity))

  override def decodeValue(byteBuf: ByteBuffer): AlarmSeverity = read[AlarmSeverity](utf8StringCodec.decodeKey(byteBuf))
}
