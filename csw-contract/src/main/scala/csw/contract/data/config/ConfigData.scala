package csw.contract.data.config

import java.net.URI
import java.nio.file.Paths
import java.time.Instant

import csw.config.models.{ConfigFileInfo, ConfigFileRevision, ConfigId, ConfigMetadata}

trait ConfigData {
  val configId: ConfigId                     = ConfigId("config1")
  private val author                         = "author_name"
  private val comment                        = "some comment"
  val configFileInfo: ConfigFileInfo         = ConfigFileInfo(Paths.get(new URI("file:///some-path")), configId, author, comment)
  val configFileRevision: ConfigFileRevision = ConfigFileRevision(configId, author, comment, Instant.now)
  val configMetadata: ConfigMetadata         = ConfigMetadata("/path", "/annexPath", "10 MiB", "20 MiB")
}
