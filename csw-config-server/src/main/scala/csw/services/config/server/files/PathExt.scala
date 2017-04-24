package csw.services.config.server.files

import java.nio.file.Path

import csw.services.config.api.exceptions.InvalidFilePath

object PathExt {

  private val invalidChars = "!#$%&'@^`~+,;="

  private def isInvalidPath(path: Path): Boolean = {
    val invalidCharsPattern = s".*[$invalidChars\\s]+.*"
    path.toString.matches(invalidCharsPattern)
  }

  implicit class RichPath(val path: Path) extends AnyVal {
    def validate =
      if (isInvalidPath(path))
        throw new InvalidFilePath(path, s"${invalidChars.mkString(" ")} 'whitespaces'")
  }
}
