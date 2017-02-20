package org.tmt.csw.location

object Hello extends Greeting with App {
}

trait Greeting {
  lazy val greeting: String = "hello"
}
