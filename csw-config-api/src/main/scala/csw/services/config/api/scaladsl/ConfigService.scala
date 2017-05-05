package csw.services.config.api.scaladsl

import java.nio.file.Path
import java.time.Instant

import csw.services.config.api.commons.FileType
import csw.services.config.api.models._

import scala.concurrent.Future

/**
 * Defines an interface for storing and retrieving configuration information
 */
trait ConfigService extends ConfigClientService {

  /**
   * Creates a file with the given path and data and optional comment.
   * An IOException is thrown if the file already exists.
   *
   * @param path       the file path relative to the repository root
   * @param configData used to read the contents of the file
   * @param annex   true if the file is annex and requires special handling (external storage)
   * @param comment    an optional comment to associate with this file
   * @return a unique id that can be used to refer to the file
   */
  def create(path: Path, configData: ConfigData, annex: Boolean = false, comment: String = ""): Future[ConfigId]

  /**
   * Updates the config file with the given path and data and optional comment.
   * An FileNotFoundException is thrown if the file does not exists.
   *
   * @param path       the file path relative to the repository root
   * @param configData used to read the contents of the file
   * @param comment    an optional comment to associate with this file
   * @return a unique id that can be used to refer to the file
   */
  def update(path: Path, configData: ConfigData, comment: String = ""): Future[ConfigId]

  /**
   * Gets and returns the file stored under the given path.
   *
   * @param path the file path relative to the repository root
   * @param id   id used to specify a specific version to fetch
   * @return a future object that can be used to access the file's data, if found
   */
  def getById(path: Path, id: ConfigId): Future[Option[ConfigData]]

  /**
   * Gets and returns the file stored under the given path.
   *
   * @param path the file path relative to the repository root
   * @return a future object that can be used to access the file's data, if found
   */
  def getLatest(path: Path): Future[Option[ConfigData]]

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
  def getByTime(path: Path, time: Instant): Future[Option[ConfigData]]

  /**
   * Deletes the given config file (older versions will still be available)
   *
   * @param path the file path relative to the repository root
   */
  def delete(path: Path, comment: String = "deleted"): Future[Unit]

  /**
   * Returns a list containing all of the known config files
   *
   * @return a list containing one ConfigFileInfo object for each known config file
   */
  def list(fileType: Option[FileType] = None, pattern: Option[String] = None): Future[List[ConfigFileInfo]]

  /**
   * Returns a list of all known versions of a given path
   *
   * @param path       the file path relative to the repository root
   * @param maxResults the maximum number of history results to return (default: unlimited)
   * @return a list containing one ConfigFileRevision object for each version of path
   */
  def history(path: Path, maxResults: Int = Int.MaxValue): Future[List[ConfigFileRevision]]

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
  def setActiveVersion(path: Path, id: ConfigId, comment: String = ""): Future[Unit]

  /**
   * Resets the "active version" of the file with the given path to the latest version.
   *
   * @param path the file path relative to the repository root
   * @return     a future result
   */
  def resetActiveVersion(path: Path, comment: String = ""): Future[Unit]

  /**
   * Returns the version which represents the "active version" of the file with the given path
   *
   * @param path the file path relative to the repository root
   * @return     id which represents the current active version
   */
  def getActiveVersion(path: Path): Future[ConfigId]

  /**
   * Gets and returns the active version of the file stored under the given path.
   * If no active was set, this returns the version with which the file was created.
   *
   * @param path the file path relative to the repository root
   * @return     a future object that can be used to access the file's data, if found
   */
  def getActiveByTime(path: Path, time: Instant): Future[Option[ConfigData]]

  /**
   * Query the metadata of config server
   *
   * @return     future of object containing config server's metadata
   */
  def getMetadata: Future[ConfigMetadata]

}
