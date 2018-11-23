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
 * @param msg exception message containing invalid pattern
 */
case class InvalidInput(msg: String) extends RuntimeException(msg)

/**
 * An Exception representing Unauthenticated user performing an action
 */
case object NotAllowed extends RuntimeException("Not allowed, make sure you have correct role assigned to perform this action.")

/**
 * An Exception representing Unauthorized user performing an action
 */
case object Unauthorized extends RuntimeException("Unauthorized, You must login before performing this action.")

/**
 * An Exception representing undefined response content
 */
case object EmptyResponse extends RuntimeException("response must have content-length")

/**
 * LocalFileNotFound exception is thrown while starting the container or host app if a local config file used to spawn the
 * app is not available on local disk
 *
 * @param filePath the path of file on local disk that is not available
 */
private[config] case class LocalFileNotFound(filePath: Path)
    extends RuntimeException(s"File does not exist on local disk at path ${filePath.toString}")

/**
 * UnableToParseOptions is thrown while starting the container or host app if any of the options is not valid
 */
private[config] case object UnableToParseOptions
    extends RuntimeException("Could not parse command line options. See --help to know more.")
