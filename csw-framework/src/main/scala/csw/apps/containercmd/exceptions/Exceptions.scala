package csw.apps.containercmd.exceptions

import java.nio.file.Path

object Exceptions {
  case class FileNotFound(filePath: Path)
      extends RuntimeException(s"File does not exist in config service at path ${filePath.toString}")

  case class LocalFileNotFound(filePath: Path)
      extends RuntimeException(s"File does not exist on local disk at path ${filePath.toString}")

}
