package csw.config.api.exceptions

import java.nio.file.Path

/**
 * An Exception representing failure of creating a file which already exists at the given path relative to repository root
 *
 * @param path file path relative to repository root
 */
case class FileAlreadyExists(path: Path) extends RuntimeException(s"File already exists in repository at path=$path")

/**
 * An Exception representing file not found at the given path relative to repository root
 *
 * @param message exception message containing file path relative to repository root
 */
case class FileNotFound(message: String) extends RuntimeException(message)

object FileNotFound {
  def apply(path: Path): FileNotFound = FileNotFound(s"File does not exist at path=$path")
}

/**
 * An Exception representing failed validation of provided input
 *
 * @param message exception message containing invalid pattern
 */
case class InvalidInput(message: String) extends RuntimeException(message)

/**
 * An Exception representing undefined response content
 */
case object EmptyResponse
    extends RuntimeException(
      "response must have content-length"
    )
