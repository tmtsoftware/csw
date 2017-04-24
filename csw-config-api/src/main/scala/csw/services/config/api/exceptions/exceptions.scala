package csw.services.config.api.exceptions

import java.nio.file.Path

case class FileAlreadyExists(path: Path) extends RuntimeException(s"File already exists in repository at path=$path")
case class FileNotFound(path: Path)      extends RuntimeException(s"File does not exist at path=$path")
case class InvalidFilePath(message: String) extends RuntimeException(message) {
  def this(path: Path, invalidChars: String) = this(
    s"Input file path '$path' contains invalid characters. Note, these characters $invalidChars 'whitespaces' are not allowed in file path."
  )
}
