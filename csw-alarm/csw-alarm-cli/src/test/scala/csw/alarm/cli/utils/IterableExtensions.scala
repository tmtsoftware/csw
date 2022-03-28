/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.cli.utils

import scala.io.Source
import org.scalatest.matchers.should.Matchers

object IterableExtensions extends Matchers {

  implicit class RichStringIterable(buffer: Iterable[String]) {
    def shouldEqualContentsOf(fileName: String): Unit = {
      val expected = Source.fromResource(fileName).getLines().filterNot(_.contains("Alarm Time"))
      val actual   = buffer.flatMap(_.split("\n")).filterNot(_.contains("Alarm Time"))

      actual.mkString("\n") shouldEqual expected.mkString("\n")
    }
  }

}
