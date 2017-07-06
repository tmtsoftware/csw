package csw.services.config.server.files

import java.nio.file.{Path, Paths}

import akka.stream.scaladsl.{FileIO, Keep}
import csw.services.config.api.models.ConfigData
import csw.services.config.server.commons.ConfigServerLogger
import csw.services.config.server.{ActorRuntime, Settings}

import scala.async.Async._
import scala.concurrent.Future

/**
 * The files are stored in the configured directory using a file name and directory structure
 * based on the SHA-1 hash of the file contents (This is the same way Git stores data).
 * The file checked in to the Svn repository is then named ''file''.`sha1` and contains only
 * the SHA-1 hash value.
  **/
class AnnexFileService(settings: Settings, fileRepo: AnnexFileRepo, actorRuntime: ActorRuntime)
    extends ConfigServerLogger.Simple {

  import actorRuntime._

  def post(configData: ConfigData): Future[String] = async {
    log.debug("Creating temporary file and calculating it's sha")
    val (tempFilePath, sha) = await(saveAndSha(configData))

    val outPath = makePath(settings.`annex-files-dir`, sha)

    if (await(fileRepo.exists(outPath))) {
      log.debug(s"Annex file already exists at path ${outPath.toString}")
      await(fileRepo.delete(tempFilePath))
      sha
    } else {
      log.debug(s"Creating directory at ${outPath.getParent.toString}")
      await(fileRepo.createDirectories(outPath.getParent))
      await(fileRepo.move(tempFilePath, outPath))
      log.debug("Validating if annex file is created with intended contents")
      if (await(validate(sha, outPath))) {
        sha
      } else {
        log.debug(
            s"Deleting annex file from path ${outPath.toString} and temporary file from path ${tempFilePath.toString}")
        await(fileRepo.delete(outPath))
        await(fileRepo.delete(tempFilePath))
        throw new RuntimeException(s" Error in creating file for $sha")
      }
    }
  }

  def get(sha: String): Future[Option[ConfigData]] = async {
    val repoFilePath = makePath(settings.`annex-files-dir`, sha)

    log.debug(s"Checking if annex file exists at ${repoFilePath.toString}")
    if (await(fileRepo.exists(repoFilePath))) {
      Some(ConfigData.fromPath(repoFilePath))
    } else {
      None
    }
  }

  // Returns the name of the file to use in the configured directory.
  // Like Git, distribute the files in directories based on the first 2 chars of the SHA-1 hash
  private def makePath(dir: String, file: String): Path = {
    log.debug(s"Making annex file path with directory $dir and filename $file")
    val (subdir, name) = file.splitAt(2)
    Paths.get(dir, subdir, name)
  }

  /**
   * Verifies that the given file's content matches the SHA-1 id
   *
   * @param id   the SHA-1 of the file
   * @param path the file to check
   * @return true if the file is valid
   */
  def validate(id: String, path: Path): Future[Boolean] = async {
    id == await(Sha1.fromPath(path))
  }

  def saveAndSha(configData: ConfigData): Future[(Path, String)] = async {
    val path = await(fileRepo.createTempFile("config-service-overize-", ".tmp"))
    val (resultF, shaF) = configData.source
      .alsoToMat(FileIO.toPath(path))(Keep.right)
      .toMat(Sha1.sink)(Keep.both)
      .run()
    await(resultF).status.get
    (path, await(shaF))
  }

}
