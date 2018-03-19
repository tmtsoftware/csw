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
   * @param path the file path relative to the repository root
   * @param id revision of the file
   * @return a future that completes with true if the file exists, false otherwise. It can fail with
   *         [[csw.services.config.api.exceptions.InvalidInput]] or [[csw.services.config.api.exceptions.FileNotFound]]
   *         or [[RuntimeException]]
   */
  def exists(path: Path, id: Option[ConfigId] = None): Future[Boolean]

  /**
   * Gets and returns the content of active version of the file stored under the given path.
   *
   * @param path the file path relative to the repository root
   * @return a future object that can be used to access the file's data, if found or fails with an
   *         [[csw.services.config.api.exceptions.EmptyResponse]] or [[csw.services.config.api.exceptions.InvalidInput]]
   *         or [[csw.services.config.api.exceptions.FileNotFound]] or [[RuntimeException]]
   */
  def getActive(path: Path): Future[Option[ConfigData]]
}
