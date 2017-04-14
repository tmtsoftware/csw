package csw.services.config.server

import java.io.File
import java.nio.file.Paths

import com.typesafe.config.Config
import org.tmatesoft.svn.core.SVNURL

class Settings(config: Config) {

  private val `csw-config-server` = config.getConfig("csw-config-server")

  val `repository-dir`: String = `csw-config-server`.getString("repository-dir")
  val `oversize-files-dir`: String = `csw-config-server`.getString("oversize-files-dir")
  val `svn-user-name`: String = `csw-config-server`.getString("svn-user-name")
  val `sha1-suffix`: String = `csw-config-server`.getString("sha1-suffix")
  val `default-suffix`: String = `csw-config-server`.getString("default-suffix")
  val `service-port`: Int = `csw-config-server`.getInt("service-port")
  val `blocking-io-dispatcher`: String = `csw-config-server`.getString("blocking-io-dispatcher")

  val repositoryFile: File = Paths.get(`repository-dir`).toFile
  val svnUrl: SVNURL = SVNURL.fromFile(repositoryFile)

}
