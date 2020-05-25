package csw.logging.client.cbor

import akka.actor.ExtendedActorSystem
import csw.commons.CborAkkaSerializer
import csw.logging.models.codecs.LoggingCodecs._
import csw.logging.models.codecs.LoggingSerializable
import csw.logging.models.{Level, LogMetadata}

class LoggingAkkaSerializer(_system: ExtendedActorSystem) extends CborAkkaSerializer[LoggingSerializable](_system) {
  override def identifier: Int = 19925

  register[LogMetadata]
  register[Level]
}
