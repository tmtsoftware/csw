package csw.services.config.server.commons

import java.nio.file.Path

import csw.services.config.api.exceptions.InvalidFilePath

object PathExt {

  private val invalidChars = "!#$%&'@^`~+,;=\\s"

  private def isInvalidPath(path: Path): Boolean = {
    val invalidCharsPattern = s".*[$invalidChars]+.*"
    path.toString.matches(invalidCharsPattern)
  }

  implicit class RichPath(val path: Path) extends AnyVal {

    def validate(): Unit =
      if (isInvalidPath(path)) {
        val invalidCharsMessage = invalidChars
          .replace("\\s", " ")
          .map(char ⇒ s"{$char}")
          .mkString(",")

        throw new InvalidFilePath(path, invalidCharsMessage)
      }
  }
}
