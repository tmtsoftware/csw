package csw.services.location

object Hello extends Greeting with App {
 def square(x : Int) : Int = {
   x * x
 }
}

trait Greeting {
  lazy val greeting: String = "hello"
}
