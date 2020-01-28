package csw.event.cli

import scala.io.Source
import org.scalatest.matchers.should.Matchers

object IterableExtensions extends Matchers {

  implicit class RichStringIterable(buffer: Iterable[String]) {
    def shouldEqualContentsOf(fileName: String): Unit = {
      val expected = Source.fromResource(fileName).getLines()
      buffer.mkString("\n") shouldEqual expected.mkString("\n")
    }
  }

}
