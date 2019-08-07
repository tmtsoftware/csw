package csw.config.models.codecs

import java.nio.file.{Path, Paths}
import java.time.Instant

import csw.config.models.{ConfigFileInfo, ConfigFileRevision, ConfigId, ConfigMetadata, FileType}
import csw.params.core.formats.CodecHelpers
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs._

object ConfigCodecs extends ConfigCodecs
trait ConfigCodecs {

  implicit lazy val pathCodec: Codec[Path]       = CodecHelpers.bimap[String, Path](Paths.get(_), _.toString)
  implicit lazy val instantCodec: Codec[Instant] = CodecHelpers.bimap[String, Instant](Instant.parse, _.toString)

  // don't make configIdCodec unary codec as this codec will be used in http json
  implicit lazy val configIdCodec: Codec[ConfigId]                     = deriveCodec[ConfigId]
  implicit lazy val configFileInfoCodec: Codec[ConfigFileInfo]         = deriveCodec[ConfigFileInfo]
  implicit lazy val configFileRevisionCodec: Codec[ConfigFileRevision] = deriveCodec[ConfigFileRevision]
  implicit lazy val configMetadataCodec: Codec[ConfigMetadata]         = deriveCodec[ConfigMetadata]
  implicit lazy val fileTypeCodec: Codec[FileType]                     = CodecHelpers.enumCodec[FileType]

}
