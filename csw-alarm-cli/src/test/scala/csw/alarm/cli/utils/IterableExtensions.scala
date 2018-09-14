package csw.alarm.cli.utils

import org.scalatest.Matchers

import scala.io.Source

object IterableExtensions extends Matchers {

  implicit class RichStringIterable(buffer: Iterable[String]) {
    def shouldEqualContentsOf(fileName: String): Unit = {
      val expected = Source.fromResource(fileName).getLines().filterNot(_.contains("Alarm Time"))
      val actual   = buffer.flatMap(_.split("\n")).filterNot(_.contains("Alarm Time"))

      actual.mkString("\n") shouldEqual expected.mkString("\n")
    }
  }

}
