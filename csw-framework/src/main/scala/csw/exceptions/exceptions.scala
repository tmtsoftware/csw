package csw.exceptions

import java.nio.file.Path

case class FailureStop()          extends RuntimeException
case class FailureRestart()       extends RuntimeException
case class InitializationFailed() extends RuntimeException("Component TLA failed to initialize")
case class ClusterSeedsNotFound()
    extends RuntimeException(
      "clusterSeeds setting is not specified either as env variable or system property. " +
      "Please check online documentation for this set-up."
    )
case class FileNotFound(filePath: Path)
    extends RuntimeException(s"File does not exist in config service at path ${filePath.toString}")
case class LocalFileNotFound(filePath: Path)
    extends RuntimeException(s"File does not exist on local disk at path ${filePath.toString}")
