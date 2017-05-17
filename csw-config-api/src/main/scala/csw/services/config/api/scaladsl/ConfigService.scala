package csw.services.config.api.scaladsl

import java.nio.file.Path
import java.time.Instant

import csw.services.config.api.models.{FileType, _}

import scala.concurrent.Future

/**
 * Defines an interface to be used by admin users for storing and retrieving configuration information
 */
trait ConfigService extends ConfigClientService {

  /**
   * Creates a file at the given path with given data and comment.
   * An IOException is thrown if the file already exists.
   *
   * @param path        the file path relative to the repository root
   * @param configData  contents of the file
   * @param annex       true if the file is annex and requires special handling (external storage)
   * @param comment     comment to associate with this operation
   * @return            a unique id that can be used to refer to the file
   */
  def create(path: Path, configData: ConfigData, annex: Boolean = false, comment: String): Future[ConfigId]

  /**
   * Updates the config file at the given path with given data and comment.
   * An FileNotFoundException is thrown if the file does not exists.
   *
   * @param path        the file path relative to the repository root
   * @param configData  contents of the file
   * @param comment     comment to associate with this operation
   * @return            a unique id that can be used to refer to the file
   */
  def update(path: Path, configData: ConfigData, comment: String): Future[ConfigId]

  /**
   * Gets and returns the file at the given path with the specified revision id.
   *
   * @param path        the file path relative to the repository root
   * @param id          id used to specify a specific version to fetch
   * @return            a future object that can be used to access the file's data, if found
   */
  def getById(path: Path, id: ConfigId): Future[Option[ConfigData]]

  /**
   * Gets and returns the latest file at the given path.
   *
   * @param path        the file path relative to the repository root
   * @return            a future object that can be used to access the file's data, if found
   */
  def getLatest(path: Path): Future[Option[ConfigData]]

  /**
   * Gets the file at the given path as it existed on the given instant.
   * If instant is before the file was created, the initial version is returned.
   * If instant is after the last change, the most recent version is returned.
   *
   * @param path        the file path relative to the repository root
   * @param time        the target instant
   * @return            a future object that can be used to access the file's data, if found
   */
  def getByTime(path: Path, time: Instant): Future[Option[ConfigData]]

  /**
   * Deletes the given config file (older versions will still be available)
   *
   * @param path        the file path relative to the repository root
   * @param comment     comment to associate with this operation
   */
  def delete(path: Path, comment: String): Future[Unit]

  /**
   * Returns a list containing all of the known config files of given type(Annex or Normal) and whose name matches the provided pattern
   *
   * @param fileType    optional file type(Annex or Normal)
   * @param pattern     optional pattern to match against the file name
   * @return            a list containing one ConfigFileInfo object for each known config file
   */
  def list(fileType: Option[FileType] = None, pattern: Option[String] = None): Future[List[ConfigFileInfo]]

  /**
   * Returns the history of versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path        the file path relative to the repository root
   * @param from        the start of the history range
   * @param to          the end of the history range
   * @param maxResults  the maximum number of history results to return (default: unlimited)
   * @return            a list containing one ConfigFileHistory object for each version of path
   */
  def history(path: Path,
              from: Instant = Instant.MIN,
              to: Instant = Instant.now,
              maxResults: Int = Int.MaxValue): Future[List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path        the file path relative to the repository root
   * @param from        the start of the history range
   * @param to          the end of the history range
   * @param maxResults  the maximum number of history results to return (default: unlimited)
   * @return            a list containing one ConfigFileHistory object for each version of path
   */
  def historyActive(path: Path,
                    from: Instant = Instant.MIN,
                    to: Instant = Instant.now,
                    maxResults: Int = Int.MaxValue): Future[List[ConfigFileRevision]]

  /**
   * Sets the "active version" to be the version provided for the file at the given path.
   * If this method is not called, the active version will always be the version with which the file was created
   * After calling this method, the version with the given Id will be the active version.
   *
   * @param path        the file path relative to the repository root
   * @param id          an id used to specify a specific version
   *                    (by default the id of the version with which the file was created i.e. 1)
   * @param comment     comment to associate with this operation
   * @return            a future result
   */
  def setActiveVersion(path: Path, id: ConfigId, comment: String): Future[Unit]

  /**
   * Resets the "active version" of the file at the given path to the latest version.
   *
   * @param path        the file path relative to the repository root
   * @param comment     comment to associate with this operation
   * @return            a future result
   */
  def resetActiveVersion(path: Path, comment: String): Future[Unit]

  /**
   * Returns the version which represents the "active version" of the file at the given path.
   *
   * @param path        the file path relative to the repository root
   * @return            id which represents the current active version
   */
  def getActiveVersion(path: Path): Future[Option[ConfigId]]

  /**
   * Gets and returns the content of active version of the file at the given path as it existed on the given instant
   *
   * @param path        the file path relative to the repository root
   * @param time        the target instant
   * @return            a future object that can be used to access the file's data, if found
   */
  def getActiveByTime(path: Path, time: Instant): Future[Option[ConfigData]]

  /**
   * Query the metadata of config server
   *
   * @return     future of object containing config server's metadata
   */
  def getMetadata: Future[ConfigMetadata]

}
