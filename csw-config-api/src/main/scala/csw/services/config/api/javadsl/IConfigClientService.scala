package csw.services.config.api.javadsl

import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{lang ⇒ jl}

import csw.services.config.api.models._
import csw.services.config.api.scaladsl.ConfigService

/**
 * Defines an interface to be used by clients for retrieving configuration information
 */
trait IConfigClientService {

  /**
   * Returns true if the given path exists and is being managed
   *
   * @param path the file path relative to the repository root
   * @return true if the file exists, false otherwise
   */
  def exists(path: Path): CompletableFuture[jl.Boolean]

  /**
   * Returns true if the given path exists at the given revision
   *
   * @param path the file path relative to the repository root
   * @param id revision of the file
   * @return true if the file exists, false otherwise
   */
  def exists(path: Path, id: ConfigId): CompletableFuture[jl.Boolean]

  /**
   * Gets and returns the content of active version of the file stored under the given path.
   *
   * @param path the file path relative to the repository root
   * @return a future object that can be used to access the file's data, if found
   */
  def getActive(path: Path): CompletableFuture[Optional[ConfigData]]

  /**
   * Returns the Scala API for this instance of config service
   */
  def asScala: ConfigService
}
