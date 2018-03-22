package csw.services.config.server.files

import java.nio.file._

import akka.dispatch.MessageDispatcher

import scala.concurrent.Future

/**
 * Represents file based repository for large/binary/annex files
 *
 * @param blockingIoDispatcher dispatcher to be used for blocking file operations
 */
class AnnexFileRepo(blockingIoDispatcher: MessageDispatcher) {

  private implicit val _blockingIoDispatcher: MessageDispatcher = blockingIoDispatcher

  def exists(path: Path): Future[Boolean]                          = Future(Files.exists(path))
  def delete(path: Path): Future[Unit]                             = Future(Files.delete(path))
  def createDirectories(path: Path): Future[Unit]                  = Future(Files.createDirectories(path))
  def createTempFile(prefix: String, suffix: String): Future[Path] = Future(Files.createTempFile(prefix, suffix))
  def move(source: Path, target: Path): Future[Unit]               = Future(Files.move(source, target, StandardCopyOption.ATOMIC_MOVE))

}
