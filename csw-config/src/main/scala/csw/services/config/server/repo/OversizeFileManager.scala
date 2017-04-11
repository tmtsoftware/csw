package csw.services.config.server.repo

import java.io.{File, FileOutputStream}
import java.nio.file.{Files, Path, Paths}

import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Keep}
import csw.services.config.api.commons.ShaUtils
import csw.services.config.api.models.ConfigData
import csw.services.config.server.Settings

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * The files are stored in the configured directory using a file name and directory structure
  * based on the SHA-1 hash of the file contents (This is the same way Git stores data).
  * The file checked in to the Svn repository is then named ''file''.`sha1` and contains only
  * the SHA-1 hash value.
  **/
class OversizeFileManager(settings: Settings) {

  def post(configData: ConfigData)(implicit mat: Materializer): Future[String] = async {
    val (tempFile, sha) = await(saveAndSha(configData))

    val outPath = makePath(settings.`oversize-files-dir`, sha)
    val outFile = outPath.toFile
    outFile.getParentFile.mkdirs()

    if (outFile.exists) {
      Files.delete(tempFile.toPath)
      sha
    }
    else {
      Files.move(tempFile.toPath, outFile.toPath)
      if (await(validate(sha, outFile))) {
        sha
      }
      else {
        outFile.delete()
        Files.delete(tempFile.toPath)
        throw new RuntimeException(s" Error in creating file for $sha")
      }
    }
  }

  def get(shaAsFileName: String): Option[ConfigData] = {
    val repoFilePath = makePath(settings.`oversize-files-dir`, shaAsFileName)

    if (repoFilePath.toFile.exists()) {
      Some(ConfigData(FileIO.fromPath(repoFilePath)))
    } else {
      None
    }
  }

  // Returns the name of the file to use in the configured directory.
  // Like Git, distribute the files in directories based on the first 2 chars of the SHA-1 hash
  private def makePath(dir: String, file: String): Path = {
    val (subdir, name) = file.splitAt(2)
    Paths.get(dir, subdir, name)
  }

  /**
    * Verifies that the given file's content matches the SHA-1 id
    *
    * @param id   the SHA-1 of the file
    * @param file the file to check
    * @return true if the file is valid
    */
  def validate(id: String, file: File)(implicit mat: Materializer): Future[Boolean] = async {
    id == await(ShaUtils.generateSHA1(file))
  }


  def saveAndSha(configData: ConfigData)(implicit mat: Materializer): Future[(File, String)] = async {
    val path = Files.createTempFile("config-service-overize-", ".tmp")
    val (resultF, shaF) = configData.source
      .alsoToMat(FileIO.toPath(path))(Keep.right)
      .toMat(ShaUtils.sha1Sink)(Keep.both)
      .run()
    await(resultF).status.get
    (path.toFile, await(shaF))
  }

}
