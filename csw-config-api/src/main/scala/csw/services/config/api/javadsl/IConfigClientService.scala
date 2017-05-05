package csw.services.config.api.javadsl

import java.nio.file.Path
import java.time.Instant
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.{lang ⇒ jl, util ⇒ ju}

import csw.services.config.api.models._
import csw.services.config.api.scaladsl.ConfigService

/**
 * Defines an interface for storing and retrieving configuration information
 */
trait IConfigClientService {

  /**
   * Returns true if the given path exists and is being managed
   *
   * @param path the file path relative to the repository root
   * @return     true the file exists
   */
  def exists(path: Path): CompletableFuture[jl.Boolean]

  /**
   * Returns true if the given path exists and is being managed
   *
   * @param path the file path relative to the repository root
   * @param id   revision of the file
   * @return     true the file exists
   */
  def exists(path: Path, id: Optional[ConfigId]): CompletableFuture[jl.Boolean]

  /**
   * Gets and returns the active version of the file stored under the given path.
   * If no active was set, this returns the version with which the file was created i.e. 1.
   *
   * @param path the file path relative to the repository root
   * @return     a future object that can be used to access the file's data, if found
   */
  def getActive(path: Path): CompletableFuture[Optional[ConfigData]]

  /**
   * Returns the Scala API for this instance of config service
   */
  def asScala: ConfigService
}
