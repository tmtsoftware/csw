package csw.services.event.cli

import org.scalatest.Matchers

import scala.collection.mutable
import scala.io.Source

object BufferExtensions extends Matchers {
  implicit class RichBuffer(buffer: mutable.Buffer[String]) {
    def shouldEqualContentsOf(fileName: String): Unit = {
      val expected = Source.fromResource(fileName).getLines()
      buffer.mkString("\n") shouldEqual expected.mkString("\n")
    }
  }
}
