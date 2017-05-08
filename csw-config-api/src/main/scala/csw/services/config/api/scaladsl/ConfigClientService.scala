package csw.services.config.api.scaladsl

import java.nio.file.Path

import csw.services.config.api.models._

import scala.concurrent.Future

/**
 * Defines an interface for storing and retrieving configuration information
 */
trait ConfigClientService {

  /**
   * Returns true if the given path exists and is being managed
   *
   * @param path the file path relative to the repository root
   * @param id revision of the file
   * @return true the file exists
   */
  def exists(path: Path, id: Option[ConfigId] = None): Future[Boolean]

  /**
   * Gets and returns the active version of the file stored under the given path.
   * If no active was set, this returns the version with which the file was created i.e. 1.
   *
   * @param path the file path relative to the repository root
   * @return     a future object that can be used to access the file's data, if found
   */
  def getActive(path: Path): Future[Option[ConfigData]]
}
