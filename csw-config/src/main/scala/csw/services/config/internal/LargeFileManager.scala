package csw.services.config.internal

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Path, Paths}

import akka.http.scaladsl.model._
import net.codejava.security.HashGeneratorUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
* The files are stored in the configured directory using a file name and directory structure
* based on the SHA-1 hash of the file contents (This is the same way Git stores data).
* The file checked in to the Svn repository is then named ''file''.`sha1` and contains only
* the SHA-1 hash value.
**/
class LargeFileManager(settings: Settings) {

  def post(inFile: File): Future[String] = Future {
    val sha = HashGeneratorUtils.generateSHA1(inFile)
    val dir = settings.`large-files-dir`.replaceFirst("~", System.getProperty("user.home"))

    val path = makePath(new File(dir), new File(sha))
    val outFile = path.toFile
    outFile.getParentFile.mkdirs()

    if (outFile.exists) {
      sha
    }
    else {
      val out = new FileOutputStream(outFile)
      Files.copy(inFile.toPath, out)
      out.flush()
      out.close()
      if(validate(sha, outFile)) {
        sha
      }
      else {
        outFile.delete()
        throw new RuntimeException(s" Error in creating file for $sha")
      }
    }
  }

  def get(shaAsFileName: String, outFile: File): Future[File] = Future {
    val repoDir = settings.`large-files-dir`.replaceFirst("~", System.getProperty("user.home"))
    val repoFilePath = makePath(new File(repoDir), new File(shaAsFileName))

    if(repoFilePath.toFile.exists()) {
      val out = new FileOutputStream(outFile)
      Files.copy(repoFilePath, out)
      out.flush()
      out.close()
      outFile
    } else {
      throw new RuntimeException(s" Error in locating file for $shaAsFileName")
    }
  }

  // Returns the name of the file to use in the configured directory.
  // Like Git, distribute the files in directories based on the first 2 chars of the SHA-1 hash
  private def makePath(dir: File, file: File): Path = {
    val (subdir, name) = file.getName.splitAt(2)
    Paths.get(dir.getPath, subdir, name)
  }

  /**
    * Verifies that the given file's content matches the SHA-1 id
    * @param id the SHA-1 of the file
    * @param file the file to check
    * @return true if the file is valid
    */
  def validate(id: String, file: File): Boolean =
    id == HashGeneratorUtils.generateSHA1(file)
}
