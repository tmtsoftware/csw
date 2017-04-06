package csw.services.cs.internal

import java.io.File
import java.nio.file.Paths

import org.tmatesoft.svn.core.SVNURL

class Settings {

  def file: File = Paths.get("/tmp/abc123").toFile

  def url: SVNURL = SVNURL.fromFile(file)

  def userName: String = "kaku"

  def sha1Suffix = ".sha1"

  def tmpDir = "/tmp/temp-dir-for-csw"

  def defaultSuffix = ".default"

  def dir = "/tmp/CsTestOversize"

}
