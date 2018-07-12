package csw.services.event.cli

import org.scalatest.Matchers

import scala.io.Source

object IterableExtensions extends Matchers {

  implicit class RichStringIterable(buffer: Iterable[String]) {
    def shouldEqualContentsOf(fileName: String): Unit = {
      val expected = Source.fromResource(fileName).getLines()
      buffer.mkString("\n") shouldEqual expected.mkString("\n")
    }
  }

}
