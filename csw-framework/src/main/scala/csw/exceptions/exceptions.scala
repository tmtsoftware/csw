package csw.exceptions

import java.nio.file.Path

case class FailureStop(message: String)    extends RuntimeException(message)
case class FailureRestart(message: String) extends RuntimeException(message)
case class FileNotFound(filePath: Path)
    extends RuntimeException(s"File does not exist in config service at path ${filePath.toString}")
case class LocalFileNotFound(filePath: Path)
    extends RuntimeException(s"File does not exist on local disk at path ${filePath.toString}")

case object InitializationFailed extends RuntimeException("Component TLA failed to initialize")
