package csw.services.config.server.commons

import java.util.regex.Pattern

object PathValidator {

  private val invalidChars   = "!#<>$%&'@^`~+,;=\\s"
  private val invalidPattern = Pattern.compile(s"[$invalidChars]+")

  private val invalidCharsMessage: String = invalidChars
    .replace("\\s", " ")
    .map(char ⇒ s"{$char}")
    .mkString(",")

  def message(path: String): String =
    s"Input file path '$path' contains invalid characters. Note, these characters $invalidCharsMessage are not allowed in file path."

  def isValid(path: String): Boolean = !invalidPattern.matcher(path).find()
}
