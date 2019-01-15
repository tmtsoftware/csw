package csw.config.server.files

import java.nio.file.{Path, Paths}

import akka.stream.scaladsl.{FileIO, Keep}
import csw.config.api.models.ConfigData
import csw.config.server.commons.ConfigServerLogger
import csw.config.server.{ActorRuntime, Settings}
import csw.logging.core.scaladsl.Logger

import scala.async.Async._
import scala.concurrent.Future

/**
 * The files are stored in the configured directory using a file name and directory structure
 * based on the SHA-1 hash of the file contents (This is the same way Git stores data).
 * The file checked in to the Svn repository is then named ''file''.`sha1` and contains only
 * the SHA-1 hash value.
 *
 * @param settings retrieve the directory path to store annex files from settings
 * @param fileRepo FileRepo performs file operations with a blocking dispatcher
 * @param actorRuntime ActorRuntime provides runtime accessories related to ActorSystem like Materializer, ExecutionContext etc.
 */
class AnnexFileService(settings: Settings, fileRepo: AnnexFileRepo, actorRuntime: ActorRuntime) {

  import actorRuntime._

  private val log: Logger = ConfigServerLogger.getLogger

  // The debug statements help understand the flow of the method
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
          s"Deleting annex file from path ${outPath.toString} and temporary file from path ${tempFilePath.toString}"
        )
        await(fileRepo.delete(outPath))
        await(fileRepo.delete(tempFilePath))
        throw new RuntimeException(s" Error in creating file for $sha")
      }
    }
  }

  // The debug statements help understand the flow of the method
  def get(sha: String): Future[Option[ConfigData]] = async {
    val repoFilePath = makePath(settings.`annex-files-dir`, sha)

    log.debug(s"Checking if annex file exists at ${repoFilePath.toString}")
    if (await(fileRepo.exists(repoFilePath))) {
      Some(ConfigData.fromPath(repoFilePath))
    } else {
      None
    }
  }

  /**
   * Returns the name of the file to use in the configured directory.
   * Like Git, distribute the files in directories based on the first 2 chars of the SHA-1 hash
   *
   * @param dir The parent directory of the file to be created
   * @param file The name of the file to be created
   * @return The path of the file created
   */
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
  private def validate(id: String, path: Path): Future[Boolean] = async {
    id == await(Sha1.fromPath(path))
  }

  /**
   * The stream of data is branched into two flows, one to dump it in temporary file and other to calculate incremental
   * sha of data.
   *
   * @param configData The data to be saved temporarily
   * @return The tuple of file path where data is saved temporarily and the sha calculated out of that data
   */
  private def saveAndSha(configData: ConfigData): Future[(Path, String)] = async {
    val path = await(fileRepo.createTempFile("config-service-overize-", ".tmp"))
    val (resultF, shaF) = configData.source
      .alsoToMat(FileIO.toPath(path))(Keep.right)
      .toMat(Sha1.sink)(Keep.both)
      .run()
    await(resultF).status.get
    (path, await(shaF))
  }

}
