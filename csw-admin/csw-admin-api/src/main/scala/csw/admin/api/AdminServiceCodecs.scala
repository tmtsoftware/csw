package csw.admin.api

import csw.admin.api.AdminServiceError.UnresolvedAkkaLocation
import csw.admin.api.AdminServiceHttpMessage.{GetLogMetadata, SetLogMetadata}
import csw.location.models.codecs.LocationCodecs
import csw.logging.models.codecs.LoggingCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait AdminServiceCodecs extends LoggingCodecs with LocationCodecs {
  lazy val getLogMetadataCodec: Codec[GetLogMetadata] = deriveCodec
  lazy val setLogMetadataCodec: Codec[SetLogMetadata] = deriveCodec

  //errors
  lazy val unresolvedAkkaLocation: Codec[UnresolvedAkkaLocation] = deriveCodec
}
