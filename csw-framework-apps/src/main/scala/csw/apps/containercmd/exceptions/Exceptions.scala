package csw.apps.containercmd.exceptions

import java.nio.file.Path

object Exceptions {
  case class FileNotFound(filePath: Path)
      extends RuntimeException(s"File does not exist in config service at path ${filePath.toString}")

  case class FileDataNotFound(filePath: Path)
      extends RuntimeException(s"No Data received for file at path ${filePath.toString}")

}
