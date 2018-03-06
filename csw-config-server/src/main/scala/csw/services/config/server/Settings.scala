package csw.services.config.server

import java.io.File
import java.nio.file.Paths

import com.typesafe.config.Config
import org.tmatesoft.svn.core.SVNURL

/**
 * Represents the runtime configuration for config server
 *
 * @param config config representing various properties to start config server
 */
class Settings(config: Config) {

  private val `csw-config-server`        = config.getConfig("csw-config-server")
  private val `akka.http.server.parsing` = config.getConfig("akka.http.server.parsing")

  val `repository-dir`: String         = `csw-config-server`.getString("repository-dir")
  val `annex-files-dir`: String        = `csw-config-server`.getString("annex-dir")
  val `svn-user-name`: String          = `csw-config-server`.getString("svn-user-name")
  val `sha1-suffix`: String            = `csw-config-server`.getString("sha1-suffix")
  val `active-config-suffix`: String   = `csw-config-server`.getString("active-config-suffix")
  def `service-port`: Int              = `csw-config-server`.getInt("service-port")
  val `blocking-io-dispatcher`: String = `csw-config-server`.getString("blocking-io-dispatcher")
  val `annex-min-file-size`: Long      = `csw-config-server`.getBytes("annex-min-file-size")
  val `max-content-length`: String     = `akka.http.server.parsing`.getString("max-content-length")

  val repositoryFile: File               = Paths.get(`repository-dir`).toFile
  val svnUrl: SVNURL                     = SVNURL.fromFile(repositoryFile)
  val annexMinFileSizeAsMetaInfo: String = `csw-config-server`.getString("annex-min-file-size")
}
