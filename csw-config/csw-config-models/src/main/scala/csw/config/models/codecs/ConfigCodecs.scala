package csw.config.models.codecs

import java.nio.file.{Path, Paths}
import java.time.Instant

import csw.config.models.{ConfigFileInfo, ConfigFileRevision, ConfigId, ConfigMetadata}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

object ConfigCodecs extends ConfigCodecs
trait ConfigCodecs {

  implicit lazy val pathCodec: Codec[Path]       = Codec.bimap[String, Path](_.toString, Paths.get(_))
  implicit lazy val instantCodec: Codec[Instant] = Codec.bimap[String, Instant](_.toString, Instant.parse)

  // don't make configIdCodec unary codec as this codec will be used in http json
  implicit lazy val configIdCodec: Codec[ConfigId]                     = deriveCodec[ConfigId]
  implicit lazy val configFileInfoCodec: Codec[ConfigFileInfo]         = deriveCodec[ConfigFileInfo]
  implicit lazy val configFileRevisionCodec: Codec[ConfigFileRevision] = deriveCodec[ConfigFileRevision]
  implicit lazy val configMetadataCodec: Codec[ConfigMetadata]         = deriveCodec[ConfigMetadata]
}
