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
   * @param path the file path relative to the repository root
   * @param configData contents of the file
   * @param annex true if the file requires special handling (external storage)
   * @param comment comment to associate with this operation
   * @return a CompletableFuture that completes with unique id that can be used to refer to the file or fail with
   *         [[csw.services.config.api.exceptions.FileAlreadyExists]] or [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def create(path: Path, configData: ConfigData, annex: Boolean, comment: String): CompletableFuture[ConfigId]

  /**
   * Creates a file at the given path with given data and comment. The file is stored normally(annex is considered as false by default)
   * An IOException is thrown if the file already exists.
   *
   * @param path the file path relative to the repository root
   * @param configData contents of the file
   * @param comment comment to associate with this operation
   * @return a CompletableFuture that completes with unique id that can be used to refer to the file or fail with
   *         [[csw.services.config.api.exceptions.FileAlreadyExists]] or [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def create(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId]

  /**
   * Updates the config file at the given path with given data and comment.
   * An FileNotFoundException is thrown if the file does not exists.
   *
   * @param path the file path relative to the repository root
   * @param configData contents of the file
   * @param comment comment to associate with this file
   * @return a CompletableFuture that completes with unique id that can be used to refer to the file or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def update(path: Path, configData: ConfigData, comment: String): CompletableFuture[ConfigId]

  /**
   * Gets and returns the file at the given path with the specified revision id.
   *
   * @param path the file path relative to the repository root
   * @param id an id used to specify a specific version to fetch
   * @return a CompletableFuture that can be used to access the file's data, if found or fails with
   *         [[csw.services.config.api.exceptions.EmptyResponse]] or [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def getById(path: Path, id: ConfigId): CompletableFuture[Optional[ConfigData]]

  /**
   * Gets and returns the latest file at the given path.
   *
   * @param path the file path relative to the repository root
   * @return a CompletableFuture that can be used to access the file's data, if found or fails with
   *         [[csw.services.config.api.exceptions.EmptyResponse]] or [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def getLatest(path: Path): CompletableFuture[Optional[ConfigData]]

  /**
   * Gets the file at the given path as it existed on the given instant.
   * If instant is before the file was created, the initial version is returned.
   * If instant is after the last change, the most recent version is returned.
   *
   * @param path the file path relative to the repository root
   * @param time the target date
   * @return a CompletableFuture that can be used to access the file's data, if found or fails with
   *         [[csw.services.config.api.exceptions.EmptyResponse]] or [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def getByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]]

  /**
   * Deletes the given config file (older versions will still be available)
   *
   * @param path the file path relative to the repository root
   * @param comment comment to associate with this operation
   * @return a CompletableFuture that completes on deletion of file or fails with [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def delete(path: Path, comment: String): CompletableFuture[Unit]

  /**
   * Returns a list containing all of the known config files of given type(Annex or Normal) and whose name matches the provided pattern
   *
   * @param fileType the file type(Annex or Normal)
   * @param pattern pattern to match against the file name
   * @return a CompletableFuture that completes ith a list containing one ConfigFileInfo object for each known config file or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def list(fileType: FileType, pattern: String): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns a list containing all of the known config files of given type(Annex or Normal)
   *
   * @param fileType the file type(Annex or Normal)
   * @return a CompletableFuture that completes ith a list containing one ConfigFileInfo object for each known config file or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def list(fileType: FileType): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns a list containing all of the known config files whose name matches the provided pattern
   *
   * @param pattern pattern to match against the file name
   * @return a CompletableFuture that completes ith a list containing one ConfigFileInfo object for each known config file or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def list(pattern: String): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns a list containing all of the known config files
   *
   * @return a CompletableFuture that completes ith a list containing one ConfigFileInfo object for each known config file or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def list(): CompletableFuture[ju.List[ConfigFileInfo]]

  /**
   * Returns the history of versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path the file path relative to the repository root
   * @param from the start of the history range
   * @param to the end of the history range
   * @param maxResults the maximum number of history results to return (default: unlimited)
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def history(path: Path, from: Instant, to: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period specified by `from` and `to`.
   *
   * @param path the file path relative to the repository root
   * @param from the start of the history range
   * @param to the end of the history range
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def history(path: Path, from: Instant, to: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path. The size of the list is limited upto `maxResults`.
   *
   * @param path the file path relative to the repository root
   * @param maxResults the maximum number of history results to return
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def history(path: Path, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at given path
   *
   * @param path the file path relative to the repository root
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def history(path: Path): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period starting from `from` till the current instant.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path the file path relative to the repository root
   * @param from the start of the history range
   * @param maxResults the maximum number of results to return
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyFrom(path: Path, from: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period starting from `from` till the current instant.
   *
   * @param path the file path relative to the repository root
   * @param from the start of the history range
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyFrom(path: Path, from: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path the file path relative to the repository root
   * @param upTo the end of the history range
   * @param maxResults the maximum number of history results to return (default: unlimited)
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyUpTo(path: Path, upTo: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of versions of the file at the given path for a range of period starting from the beginning and upto `upTo`.
   *
   * @param path the file path relative to the repository root
   * @param upTo the end of the history range
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyUpTo(path: Path, upTo: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path the file path relative to the repository root
   * @param from the start of the history range
   * @param to the end of the history range
   * @param maxResults the maximum number of history results to return (default: unlimited)
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyActive(path: Path, from: Instant, to: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period specified by `from` and `to`.
   *
   * @param path the file path relative to the repository root
   * @param from the start of the history range
   * @param to the end of the history range
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyActive(path: Path, from: Instant, to: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path. The size of the list is limited upto `maxResults`.
   *
   * @param path the file path relative to the repository root
   * @param maxResults the maximum number of history results to return
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyActive(path: Path, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at given path
   *
   * @param path the file path relative to the repository root
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyActive(path: Path): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period starting from `from` till the current instant.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path the file path relative to the repository root
   * @param from the start of the history range
   * @param maxResults the maximum number of results to return
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyActiveFrom(path: Path, from: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period starting from `from` till the current instant.
   *
   * @param path the file path relative to the repository root
   * @param from the start of the history range
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyActiveFrom(path: Path, from: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period specified by `from` and `to`.
   * The size of the list is limited upto `maxResults`.
   *
   * @param path the file path relative to the repository root
   * @param upTo the end of the history range
   * @param maxResults the maximum number of history results to return (default: unlimited)
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyActiveUpTo(path: Path, upTo: Instant, maxResults: Int): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Returns the history of active versions of the file at the given path for a range of period starting from the beginning and upto `upTo`.
   *
   * @param path the file path relative to the repository root
   * @param upTo the end of the history range
   * @return a CompletableFuture that completes with a list containing one ConfigFileHistory object for each version of path or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def historyActiveUpTo(path: Path, upTo: Instant): CompletableFuture[ju.List[ConfigFileRevision]]

  /**
   * Sets the "active version" to be the version provided for the file at the given path.
   * If this method is not called, the active version will always be the version with which the file was created
   * After calling this method, the version with the given Id will be the active version.
   *
   * @param path the file path relative to the repository root
   * @param id an id used to specify a specific version
   *           (by default the id of the version with which the file was created i.e. 1)
   * @param comment comment to associate with this operation
   * @return a CompletableFuture that completes when active version is set or fails with [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def setActiveVersion(path: Path, id: ConfigId, comment: String): CompletableFuture[Unit]

  /**
   * Resets the "active version" of the file at the given path to the latest version.
   *
   * @param path the file path relative to the repository root
   * @param comment comment to associate with this operation
   * @return a CompletableFuture that completes when active version is reset or fails with [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def resetActiveVersion(path: Path, comment: String): CompletableFuture[Unit]

  /**
   * Returns the version which represents the "active version" of the file at the given path.
   *
   * @param path the file path relative to the repository root
   * @return a CompletableFuture that completes with id representing the current active version or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def getActiveVersion(path: Path): CompletableFuture[Optional[ConfigId]]

  /**
   * Gets and returns the content of active version of the file at the given path as it existed on the given instant
   *
   * @param path the file path relative to the repository root
   * @param time the target instant
   * @return a CompletableFuture object that can be used to access the file's data, if found or fails with
   *         [[csw.services.config.api.exceptions.EmptyResponse]] or [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def getActiveByTime(path: Path, time: Instant): CompletableFuture[Optional[ConfigData]]

  /**
   * Query the metadata of config server
   *
   * @return a CompletableFuture that completes with an object containing config server's metadata or fails with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def getMetadata: CompletableFuture[ConfigMetadata]

  /**
   * Returns the Scala API for this instance of config service
   */
  def asScala: ConfigService
}
