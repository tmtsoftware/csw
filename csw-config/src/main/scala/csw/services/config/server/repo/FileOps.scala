package csw.services.config.server.repo

import java.nio.file.{Files, Path}

import akka.dispatch.MessageDispatcher

import scala.concurrent.Future

class FileOps(dispatcher: MessageDispatcher) {

  private implicit val blockingIoDispatcher = dispatcher

  def exists(path: Path): Future[Boolean] = Future(Files.exists(path))
  def delete(path: Path): Future[Unit] = Future(Files.delete(path))
  def createDirectories(path: Path): Future[Unit] = Future(Files.createDirectories(path))
  def move(source: Path, target: Path): Future[Unit] = Future(Files.move(source, target))
  def createTempFile(prefix: String, suffix: String): Future[Path] = Future(Files.createTempFile(prefix, suffix))
}
