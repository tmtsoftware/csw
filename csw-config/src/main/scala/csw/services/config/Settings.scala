package csw.services.config

import java.io.File
import java.nio.file.Paths

import com.typesafe.config.Config
import org.tmatesoft.svn.core.SVNURL

class Settings(config: Config) {

  private val `csw-config` = config.getConfig("csw-config")

  val `repository-dir`: String = `csw-config`.getString("repository-dir")
  val `tmp-dir`: String = `csw-config`.getString("tmp-dir")
  val `oversize-files-dir`: String = `csw-config`.getString("oversize-files-dir")
  val `svn-user-name`: String = `csw-config`.getString("svn-user-name")
  val `sha1-suffix`: String = `csw-config`.getString("sha1-suffix")
  val `default-suffix`: String = `csw-config`.getString("default-suffix")

  val repositoryFile: File = Paths.get(`repository-dir`).toFile
  val svnUrl: SVNURL = SVNURL.fromFile(repositoryFile)

}
