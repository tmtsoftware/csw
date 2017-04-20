package csw.services.config.api.exceptions

import java.nio.file.Path

case class FileAlreadyExists(path: Path) extends RuntimeException(s"File already exists in repository at path=$path")
case class FileNotFound(path: Path)      extends RuntimeException(s"File does not exist at path=$path")
