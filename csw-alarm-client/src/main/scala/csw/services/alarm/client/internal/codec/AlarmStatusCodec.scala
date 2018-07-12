package csw.services.alarm.client.internal.codec

import java.nio.ByteBuffer

import csw.services.alarm.api.models.{AlarmKey, AlarmStatus}
import io.lettuce.core.codec.{RedisCodec, Utf8StringCodec}
import upickle.default._

/**
 * Encodes and decodes keys as AlarmKey and values as Json byte equivalent of AlarmStatus
 */
object AlarmStatusCodec extends RedisCodec[AlarmKey, AlarmStatus] {

  private lazy val utf8StringCodec = new Utf8StringCodec()

  override def encodeKey(key: AlarmKey): ByteBuffer = utf8StringCodec.encodeKey(key.statusKey)

  override def decodeKey(byteBuf: ByteBuffer): AlarmKey = AlarmKey.fromStatusKey(utf8StringCodec.decodeKey(byteBuf))

  override def encodeValue(alarmStatus: AlarmStatus): ByteBuffer = utf8StringCodec.encodeValue(write(alarmStatus))

  override def decodeValue(byteBuf: ByteBuffer): AlarmStatus = read[AlarmStatus](utf8StringCodec.decodeKey(byteBuf))
}
