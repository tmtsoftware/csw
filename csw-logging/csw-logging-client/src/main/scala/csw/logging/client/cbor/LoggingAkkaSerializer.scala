package csw.logging.client.cbor

import csw.commons.CborAkkaSerializer
import csw.logging.models.codecs.LoggingCodecs._
import csw.logging.models.codecs.LoggingSerializable
import csw.logging.models.{Level, LogMetadata}

class LoggingAkkaSerializer extends CborAkkaSerializer[LoggingSerializable] {
  override def identifier: Int = 19925

  register[LogMetadata]
  register[Level]
}
