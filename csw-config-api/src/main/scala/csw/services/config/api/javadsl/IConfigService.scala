package csw.services.config.api.javadsl

import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{util ⇒ ju}

import csw.services.config.api.models._
import csw.services.config.api.scaladsl.ConfigService

/**
 * Defines an interface to be used by admin users for storing and retrieving configuration information
 */
trait IConfigService extends IConfigClientService {

  /**
   * Creates a file at the given path with given data and comment.
   * An IOException is thrown if the file already exists.
   *
   * @param path        The file path relative to the repository root
   * @param configData  Contents of the file
   * @param annex       True if the file requires special handling (external storage)
   * @param comment     Comment to associate with this operation
   * @return            A unique id that can be used to refer to the file
   */
  def create(path: Path, configData: ConfigData, annex: Boolean, comment: String): CompletableFuture[ConfigId]

  /**
   * Creates a file at the given path with given data and comment. The file is stored normally(annex is considered as false by default)
   * An IOException is thrown if the file already exists.
   *
   * @param path        The file path relative to the repository root
   * @param configData  Contents of the file
   * @param comment     Comment to associate with this operation
   * @return            A unique id that can be used to refer to the file
   */
  def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId]

  /**
   * Updates the config file at the given path with given data and comment.
   * An FileNotFoundException is thrown if the file does not exists.
   *
   * @param path        The file path relative to the repository root
   * @param configData  Contents of the file
   * @param comment     Comment to associate with this file
   * @return            A unique id that can be used to refer to the file
   */
  def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId]

  /**
   * Gets and returns the file at the given path with the specified revision id.
   *
   * @param path        The file path relative to the repository root
   * @param id          An id used to specify a specific version to fetch
   * @return            A future object that can be used to access the file's data, if found
   */
  def getById(path: Path, id: ConfigId): CompletableFuture[Optional[ConfigData]]

  /**
   * Gets and returns the latest file at the given path.
   *
   * @param path        The file path relative to the repository root
   * @return            A future object that can be used to access the file's data, if found
   */
  def getLatest(path: Path): CompletableFuture[Optional[ConfigData]]

  /**
   * Gets the file at the given path as it existed on the given instant.
   * If instant is before the file was created, the initial version is returned.
   * If instant is after the last change, the most recent version is returned.
   *
   * @param path        The file path relative to the repository root
   * @param time        The target date
   * @return            A future object that can be used to access the file's data, if found
   */
  def getByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]]

  /**
   * Deletes the given config file (older versions will still be available)
   *
   * @param path        The file path relative to the repository root
   * @param comment     Comment to associate with this operation
   */
  def delete(path: Path, comment: String): CompletableFuture[Unit]

  /**
   * Returns a list containing all of the known config files of given type(Annex or Normal) and whose name matches the provided pattern
   *
   * @param fileType    The file type(Annex or Normal)
   * @param pattern     Pattern to match against the file name
   * @return            A list containing one ConfigFileInfo object for each known config file
   */
  def list(fileType: FileType, pattern: String): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns a list containing all of the known config files of given type(Annex or Normal)
   *
   * @param fileType    The file type(Annex or Normal)
   * @return            A list containing one ConfigFileInfo object for each config file
   */
  def list(fileType: FileType): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns a list containing all of the known config files whose name matches the provided pattern
   *
   * @param pattern     Pattern to match against the file name
   * @return            A list containing one ConfigFileInfo object for each config file
   */
  def list(pattern: String): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns a list containing all of the known config files
   *
   * @return            A list containing one ConfigFileInfo object for each config file
   */
  def list(): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns the history of versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path        The file path relative to the repository root
   * @param from        The start of the history range
   * @param to          The end of the history range
   * @param maxResults  The maximum number of history results to return (default: unlimited)
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def history(path: Path, from: Instant, to: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period specified by `from` and `to`.
   *
   * @param path        The file path relative to the repository root
   * @param from        The start of the history range
   * @param to          The end of the history range
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def history(path: Path, from: Instant, to: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path. The size of the list is limited upto `maxResults`.
   *
   * @param path        The file path relative to the repository root
   * @param maxResults  The maximum number of history results to return
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def history(path: Path, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at given path
   *
   * @param path        The file path relative to the repository root
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def history(path: Path): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period starting from `from` till the current instant.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path        The file path relative to the repository root
   * @param from        The start of the history range
   * @param maxResults  The maximum number of results to return
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyFrom(path: Path, from: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period starting from `from` till the current instant.
   *
   * @param path        The file path relative to the repository root
   * @param from        The start of the history range
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyFrom(path: Path, from: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path        The file path relative to the repository root
   * @param upTo        The end of the history range
   * @param maxResults  The maximum number of history results to return (default: unlimited)
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyUpTo(path: Path, upTo: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period starting from the beginning and upto `upTo`.
   *
   * @param path        The file path relative to the repository root
   * @param upTo        The end of the history range
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyUpTo(path: Path, upTo: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path        The file path relative to the repository root
   * @param from        The start of the history range
   * @param to          The end of the history range
   * @param maxResults  The maximum number of history results to return (default: unlimited)
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyActive(path: Path, from: Instant, to: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period specified by `from` and `to`.
   *
   * @param path        The file path relative to the repository root
   * @param from        The start of the history range
   * @param to          The end of the history range
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyActive(path: Path, from: Instant, to: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path. The size of the list is limited upto `maxResults`.
   *
   * @param path        The file path relative to the repository root
   * @param maxResults  The maximum number of history results to return
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyActive(path: Path, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at given path
   *
   * @param path        The file path relative to the repository root
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyActive(path: Path): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period starting from `from` till the current instant.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path        The file path relative to the repository root
   * @param from        The start of the history range
   * @param maxResults  The maximum number of results to return
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyActiveFrom(path: Path, from: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period starting from `from` till the current instant.
   *
   * @param path        The file path relative to the repository root
   * @param from        The start of the history range
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyActiveFrom(path: Path, from: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path        The file path relative to the repository root
   * @param upTo        The end of the history range
   * @param maxResults  The maximum number of history results to return (default: unlimited)
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyActiveUpTo(path: Path, upTo: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period starting from the beginning and upto `upTo`.
   *
   * @param path        The file path relative to the repository root
   * @param upTo        The end of the history range
   * @return            A list containing one ConfigFileHistory object for each version of path
   */
  def historyActiveUpTo(path: Path, upTo: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Sets the "active version" to be the version provided for the file at the given path.
   * If this method is not called, the active version will always be the version with which the file was created
   * After calling this method, the version with the given Id will be the active version.
   *
   * @param path        The file path relative to the repository root
   * @param id          An id used to specify a specific version
   *                    (by default the id of the version with which the file was created i.e. 1)
   * @param comment     Comment to associate with this operation
   * @return            A future result
   */
  def setActiveVersion(path: Path, id: ConfigId, comment: String): CompletableFuture[Unit]

  /**
   * Resets the "active version" of the file at the given path to the latest version.
   *
   * @param path        The file path relative to the repository root
   * @param comment     Comment to associate with this operation
   * @return            A future result
   */
  def resetActiveVersion(path: Path, comment: String): CompletableFuture[Unit]

  /**
   * Returns the version which represents the "active version" of the file at the given path.
   *
   * @param path        The file path relative to the repository root
   * @return            Id which represents the current active version
   */
  def getActiveVersion(path: Path): CompletableFuture[Optional[ConfigId]]

  /**
   * Gets and returns the content of active version of the file at the given path as it existed on the given instant
   *
   * @param path        The file path relative to the repository root
   * @param time        The target instant
   * @return            A future object that can be used to access the file's data, if found
   */
  def getActiveByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]]

  /**
   * Query the metadata of config server
   *
   * @return            A future object containing config server's metadata
   */
  def getMetadata: CompletableFuture[ConfigMetadata]

  /**
   * Returns the Scala API for this instance of config service
   */
  def asScala: ConfigService
}
