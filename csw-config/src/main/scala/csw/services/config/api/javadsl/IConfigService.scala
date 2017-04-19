package csw.services.config.api.javadsl

import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.time.Instant
import java.util.Optional
import java.{lang ⇒ jl, util ⇒ ju}

import csw.services.config.api.models.{ConfigData, ConfigFileHistory, ConfigFileInfo, ConfigId}
import csw.services.config.api.scaladsl.ConfigService

/**
 * Defines an interface for storing and retrieving configuration information
 */
trait IConfigService {

  /**
   * The name of this instance
   */
  def name: String

  /**
   * Creates a file with the given path and data and optional comment.
   * An IOException is thrown if the file already exists.
   *
   * @param path       the file path relative to the repository root
   * @param configData used to read the contents of the file
   * @param oversize   true if the file is oversize and requires special handling (external storage)
   * @param comment    an optional comment to associate with this file
   * @return a unique id that can be used to refer to the file
   */
  def create(path: Path, configData: ConfigData, oversize: Boolean, comment: String): CompletableFuture[ConfigId]
  def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId]
  def create(path: Path, configData: ConfigData, oversize: Boolean): CompletableFuture[ConfigId]
  def create(path: Path, configData: ConfigData): CompletableFuture[ConfigId]

  /**
   * Updates the config file with the given path and data and optional comment.
   * An FileNotFoundException is thrown if the file does not exists.
   *
   * @param path       the file path relative to the repository root
   * @param configData used to read the contents of the file
   * @param comment    an optional comment to associate with this file
   * @return a unique id that can be used to refer to the file
   */
  def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId]
  def update(path: Path, configData: ConfigData): CompletableFuture[ConfigId]

  /**
   * Gets and returns the file stored under the given path.
   *
   * @param path the file path relative to the repository root
   * @param id   an optional id used to specify a specific version to fetch
   *             (by default the latest version is returned)
   * @return a future object that can be used to access the file's data, if found
   */
  def get(path: Path, id: Optional[ConfigId]): CompletableFuture[Optional[ConfigData]]
  def get(path: Path): CompletableFuture[Optional[ConfigData]]

  /**
   * Gets the file as it existed on the given date.
   * If date is before the file was created, the initial version is returned.
   * If date is after the last change, the most recent version is returned.
   * If the path does not exist in the repo, Future[None] is returned.
   *
   * @param path the file path relative to the repository root
   * @param time the target date
   * @return a future object that can be used to access the file's data, if found
   */
  def get(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]]

  /**
   * Returns true if the given path exists and is being managed
   *
   * @param path the file path relative to the repository root
   * @return true the file exists
   */
  def exists(path: Path): CompletableFuture[jl.Boolean]

  /**
   * Returns true if the given path exists and is being managed
   *
   * @param path the file path relative to the repository root
   * @param id revision of the file
   * @return true the file exists
   */
  def exists(path: Path, id: Optional[ConfigId]): CompletableFuture[jl.Boolean]

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
  def list(): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns a list of all known versions of a given path
   *
   * @param path       the file path relative to the repository root
   * @param maxResults the maximum number of history results to return (default: unlimited)
   * @return a list containing one ConfigFileHistory object for each version of path
   */
  def history(path: Path, maxResults: Int): CompletableFuture[ju.List[ConfigFileHistory]]
  def history(path: Path): CompletableFuture[ju.List[ConfigFileHistory]]

  /**
   * Sets the "default version" of the file with the given path.
   * If this method is not called, the default version will always be the latest version.
   * After calling this method, the version with the given Id will be the default.
   *
   * @param path the file path relative to the repository root
   * @param id   an optional id used to specify a specific version
   *             (by default the id of the latest version is used)
   * @return a future result
   */
  def setDefault(path: Path, id: Optional[ConfigId]): CompletableFuture[Unit]

  /**
   * Gets and returns the default version of the file stored under the given path.
   * If no default was set, this returns the latest version.
   *
   * @param path the file path relative to the repository root
   * @return a future object that can be used to access the file's data, if found
   */
  def getDefault(path: Path): CompletableFuture[Optional[ConfigData]]

  /**
   * Returns the Scala API for this instance of config service
   */
  def asScala: ConfigService
}
