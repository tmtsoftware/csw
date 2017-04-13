package csw.services.config.server.files

import java.nio.file.{CopyOption, Files, Path, StandardCopyOption}

import akka.dispatch.MessageDispatcher

import scala.concurrent.Future

class OversizeFileRepo(blockingIoDispatcher: MessageDispatcher) {

  private implicit val _blockingIoDispatcher = blockingIoDispatcher

  def exists(path: Path): Future[Boolean] = Future(Files.exists(path))
  def delete(path: Path): Future[Unit] = Future(Files.delete(path))
  def createDirectories(path: Path): Future[Unit] = Future(Files.createDirectories(path))
  def move(source: Path, target: Path): Future[Unit] = Future(Files.move(source, target, StandardCopyOption.ATOMIC_MOVE))
  def createTempFile(prefix: String, suffix: String): Future[Path] = Future(Files.createTempFile(prefix, suffix))
}
