package csw.services.config.server.commons

import java.nio.file.Path
import java.util.regex.Pattern

object PathValidator {

  private val invalidChars          = "!#<>$%&'@^`~+,;=\\s"
  private val invalidCharsPredicate = Pattern.compile(s"[$invalidChars]+").asPredicate()

  val invalidCharsMessage: String = invalidChars
    .replace("\\s", " ")
    .map(char ⇒ s"{$char}")
    .mkString(",")

  def isValid(path: Path): Boolean =
    !invalidCharsPredicate.test(path.toString)
}
