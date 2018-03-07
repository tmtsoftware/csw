package csw.framework.exceptions

import java.nio.file.Path

//TODO: add doc for significance for all exception like where is it used etc.

abstract class FailureStop(message: String)    extends RuntimeException(message)
abstract class FailureRestart(message: String) extends RuntimeException(message)

case class FileNotFound(filePath: Path)
    extends RuntimeException(s"File does not exist in configuration service at path ${filePath.toString}")
case class LocalFileNotFound(filePath: Path)
    extends RuntimeException(s"File does not exist on local disk at path ${filePath.toString}")

case object UnableToParseOptions extends RuntimeException("Could not parse command line options. See --help to know more.")
case object ClusterSeedsNotFound
    extends RuntimeException(
      "clusterSeeds setting is not specified either as env variable or system property. " +
      "Please check online documentation for this set-up."
    )
case object InitializationFailed extends RuntimeException("Component TLA failed to initialize")
