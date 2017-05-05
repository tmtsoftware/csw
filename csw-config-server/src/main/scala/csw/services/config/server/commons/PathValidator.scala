package csw.services.config.server.commons

import java.util.regex.Pattern

object PathValidator {

  private val invalidChars   = "!#<>$%&'@^`~+,;=\\s"
  private val invalidPattern = Pattern.compile(s"[$invalidChars]+")

  val invalidCharsMessage: String = invalidChars
    .replace("\\s", " ")
    .map(char ⇒ s"{$char}")
    .mkString(",")

  def isValid(path: String): Boolean = !invalidPattern.matcher(path).find()
}
