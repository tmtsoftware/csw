package csw.config.models.codecs

import java.nio.file.{Path, Paths}
import java.time.Instant

import csw.config.models.{ConfigFileInfo, ConfigFileRevision, ConfigId, ConfigMetadata}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs._

object ConfigCodecs extends ConfigCodecs
trait ConfigCodecs {

  implicit lazy val pathCodec: Codec[Path]       = Codec.bimap[String, Path](_.toString, Paths.get(_))
  implicit lazy val instantCodec: Codec[Instant] = Codec.bimap[String, Instant](_.toString, Instant.parse)

  // don't make configIdCodec unary codec as this codec will be used in http json
  implicit lazy val configIdCodec: Codec[ConfigId]                     = deriveCodec
  implicit lazy val configFileInfoCodec: Codec[ConfigFileInfo]         = deriveCodec
  implicit lazy val configFileRevisionCodec: Codec[ConfigFileRevision] = deriveCodec
  implicit lazy val configMetadataCodec: Codec[ConfigMetadata]         = deriveCodec
}
