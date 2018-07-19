package csw.services.alarm.client.internal.codec

import java.nio.ByteBuffer

import csw.services.alarm.api.models.{AlarmKey, AlarmSeverity}
import io.lettuce.core.codec.{RedisCodec, Utf8StringCodec}
import upickle.default._

import scala.util.control.NonFatal

/**
 * Encodes and decodes keys as AlarmKey and values as Json byte equivalent of AlarmSeverity
 */
object AlarmSeverityCodec extends RedisCodec[AlarmKey, AlarmSeverity] {

  private lazy val utf8StringCodec = new Utf8StringCodec()

  override def encodeKey(key: AlarmKey): ByteBuffer = utf8StringCodec.encodeKey(key.severityKey)

  override def decodeKey(byteBuf: ByteBuffer): AlarmKey = AlarmKey.fromSeverityKey(utf8StringCodec.decodeKey(byteBuf))

  override def encodeValue(alarmSeverity: AlarmSeverity): ByteBuffer = utf8StringCodec.encodeValue(write(alarmSeverity))

  override def decodeValue(byteBuf: ByteBuffer): AlarmSeverity =
    try {
      //TODO: use explicit null check rather than exception?
      //TODO: should we do this check for all codecs including events?
      read[AlarmSeverity](utf8StringCodec.decodeValue(byteBuf))
    } catch {
      case NonFatal(_) ⇒ AlarmSeverity.Disconnected // if severity expires than cast the null returned value to Disconnected
    }
}
