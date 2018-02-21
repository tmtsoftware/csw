package tmt.shared.dsl

import org.scalatest.Matchers
import tmt.shared.services.CommandResponse

class CsDslTest extends org.scalatest.FunSuite with Matchers with ControlDsl {

//  val wiring                 = new Wiring()
  lazy val engine: EngineDsl = ???

  def x: CommandResponse = {
    println("blocking")
    CommandResponse("Some Command Response")
  }

  /* Ignoring for time being till we have some asserts here */

  ignore("dd") {
    val dsl = new CsDsl(null, null)(null)
    println(par(x, x))
  }
}
