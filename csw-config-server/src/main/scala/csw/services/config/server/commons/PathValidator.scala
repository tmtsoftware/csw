package csw.services.config.server.commons

import java.nio.file.Path

import scala.util.matching.Regex

object PathValidator {

  private val invalidChars               = "!#<>$%&'@^`~+,;=\\s"
  private val invalidCharsPattern: Regex = s".*[$invalidChars]+.*".r

  val invalidCharsMessage: String = invalidChars
    .replace("\\s", " ")
    .map(char ⇒ s"{$char}")
    .mkString(",")

  def isValid(path: Path): Boolean = {
    val predicate = invalidCharsPattern.pattern.asPredicate()
    !predicate.test(path.toString)
  }
}
