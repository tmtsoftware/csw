package org.tmt.csw.location

import org.scalatest._

class HelloSpec extends FlatSpec with Matchers {
  "The Hello object" should "say hello" in {
    Hello.greeting shouldEqual "hello"
  }

  "The Hello object" should "square 3 to 9" in {
    Hello square 3 shouldEqual 9
  }
}
