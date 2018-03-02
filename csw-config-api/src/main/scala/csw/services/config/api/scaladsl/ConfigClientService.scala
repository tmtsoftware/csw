package csw.services.config.api.scaladsl

import java.nio.file.Path

import csw.services.config.api.models._

import scala.concurrent.Future

/**
 * Defines an interface to be used by clients for retrieving configuration information
 */
trait ConfigClientService {

  /**
   * Returns true if the given path exists and is being managed
   *
   * @param path        The file path relative to the repository root
   * @param id          Revision of the file
   * @return            True if the file exists, false otherwise
   */
  def exists(path: Path, id: Option[ConfigId] = None): Future[Boolean]

  /**
   * Gets and returns the content of active version of the file stored under the given path.
   *
   * @param path        The file path relative to the repository root
   * @return            A future object that can be used to access the file's data, if found
   */
  def getActive(path: Path): Future[Option[ConfigData]]
}
