package csw.services.config.api.javadsl

import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.time.Instant
import java.util.Optional
import java.{lang ⇒ jl, util ⇒ ju}

import csw.services.config.api.commons.FileType
import csw.services.config.api.models.{ConfigData, ConfigFileInfo, ConfigFileRevision, ConfigId}
import csw.services.config.api.scaladsl.ConfigService

/**
 * Defines an interface for storing and retrieving configuration information
 */
trait IConfigService extends IConfigClientService {

  /**
   * Creates a file with the given path and data and optional comment.
   * An IOException is thrown if the file already exists.
   *
   * @param path       the file path relative to the repository root
   * @param configData used to read the contents of the file
   * @param annex      true if the file requires special handling (external storage)
   * @param comment    an optional comment to associate with this file
   * @return           a unique id that can be used to refer to the file
   */
  def create(path: Path, configData: ConfigData, annex: Boolean, comment: String): CompletableFuture[ConfigId]
  def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId]
  def create(path: Path, configData: ConfigData, annex: Boolean): CompletableFuture[ConfigId]
  def create(path: Path, configData: ConfigData): CompletableFuture[ConfigId]

  /**
   * Updates the config file with the given path and data and optional comment.
   * An FileNotFoundException is thrown if the file does not exists.
   *
   * @param path       the file path relative to the repository root
   * @param configData used to read the contents of the file
   * @param comment    an optional comment to associate with this file
   * @return           a unique id that can be used to refer to the file
   */
  def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId]
  def update(path: Path, configData: ConfigData): CompletableFuture[ConfigId]

  /**
   * Gets and returns the file stored under the given path.
   *
   * @param path the file path relative to the repository root
   * @param id   an optional id used to specify a specific version to fetch
   *             (by default the latest version is returned)
   * @return     a future object that can be used to access the file's data, if found
   */
  def getById(path: Path, id: ConfigId): CompletableFuture[Optional[ConfigData]]
  def getLatest(path: Path): CompletableFuture[Optional[ConfigData]]

  /**
   * Gets the file as it existed on the given date.
   * If date is before the file was created, the initial version is returned.
   * If date is after the last change, the most recent version is returned.
   * If the path does not exist in the repo, Future[None] is returned.
   *
   * @param path the file path relative to the repository root
   * @param time the target date
   * @return     a future object that can be used to access the file's data, if found
   */
  def getByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]]

  /**
   * Deletes the given config file (older versions will still be available)
   *
   * @param path the file path relative to the repository root
   */
  def delete(path: Path, comment: String): CompletableFuture[Unit]
  def delete(path: Path): CompletableFuture[Unit]

  /**
   * Returns a list containing all of the known config files
   *
   * @return a list containing one ConfigFileInfo object for each known config file
   */
  def list(fileType: FileType, pattern: String): CompletableFuture[ju.List[ConfigFileInfo]]
  def list(fileType: FileType): CompletableFuture[ju.List[ConfigFileInfo]]
  def list(pattern: String): CompletableFuture[ju.List[ConfigFileInfo]]
  def list(): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns a list of all known versions of a given path
   *
   * @param path       the file path relative to the repository root
   * @param maxResults the maximum number of history results to return (default: unlimited)
   * @return           a list containing one ConfigFileHistory object for each version of path
   */
  def history(path: Path, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]
  def history(path: Path): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Sets the "active version" to be the version provided for the file with the given path.
   * If this method is not called, the active version will always be the version with which the file was created i.e. 1
   * After calling this method, the version with the given Id will be the active.
   *
   * @param path the file path relative to the repository root
   * @param id   an optional id used to specify a specific version
   *             (by default the id of the version with which the file was created i.e. 1)
   * @return     a future result
   */
  def setActive(path: Path, id: ConfigId, comment: String): CompletableFuture[Unit]
  def setActive(path: Path, id: ConfigId): CompletableFuture[Unit]

  /**
   * Resets the "active version" of the file with the given path to the latest version.
   *
   * @param path the file path relative to the repository root
   * @return     a future result
   */
  def resetActive(path: Path, comment: String): CompletableFuture[Unit]
  def resetActive(path: Path): CompletableFuture[Unit]

  /**
   * Returns the Scala API for this instance of config service
   */
  def asScala: ConfigService
}
