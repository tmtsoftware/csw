package org.tmt.csw.location

object Hello extends Greeting with App {
  1 + 1
  println("")
}

trait Greeting {
  lazy val greeting: String = "hello"
}
