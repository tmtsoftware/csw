package csw.services.config.api.exceptions

import java.nio.file.Path

case class FileAlreadyExists(path: Path) extends RuntimeException(s"File already exists in repository at path=$path")

case class FileNotFound(message: String) extends RuntimeException(message)
object FileNotFound {
  def apply(path: Path): FileNotFound = FileNotFound(s"File does not exist at path=$path")
}

case class InvalidInput(message: String) extends RuntimeException(message)
