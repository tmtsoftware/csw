package csw.services.config.server.commons

import java.nio.file.Path

import scala.util.matching.Regex

object PathValidator {

  private val invalidChars = "!#<>$%&'@^`~+,;=\\s"

  val invalidCharsMessage = invalidChars
    .replace("\\s", " ")
    .map(char ⇒ s"{$char}")
    .mkString(",")

  implicit class RichPath(val path: Path) extends AnyVal {

    def isValid(): Boolean = {
      val invalidCharsPattern: Regex = s""".*[$invalidChars]+.*""".r

      path.toString match {
        case invalidCharsPattern(_ *) ⇒ false
        case _                        ⇒ true
      }
    }

  }

}
