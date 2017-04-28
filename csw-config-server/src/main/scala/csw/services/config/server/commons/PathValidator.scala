package csw.services.config.server.commons

import java.nio.file.Path

import scala.util.matching.Regex

object PathValidator {

  private val invalidChars = "!#<>$%&'@^`~+,;=\\s"

  val invalidCharsMessage: String = invalidChars
    .replace("\\s", " ")
    .map(char ⇒ s"{$char}")
    .mkString(",")

  def isValid(path: Path): Boolean = {
    val invalidCharsPattern: Regex = s".*[$invalidChars]+.*".r

    path.toString match {
      case invalidCharsPattern(_ *) ⇒ false
      case _                        ⇒ true
    }
  }
}
