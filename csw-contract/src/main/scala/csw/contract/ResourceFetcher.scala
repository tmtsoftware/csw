package csw.contract

import scala.io.Source

object ResourceFetcher {
  def getResourceAsString(name: String): String = {
    Source.fromInputStream(getClass.getResourceAsStream(name)).mkString
  }
}
