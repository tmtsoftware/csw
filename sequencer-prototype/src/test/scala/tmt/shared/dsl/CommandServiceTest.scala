package tmt.shared.dsl

import org.scalatest.Matchers
import tmt.development.Wiring
import tmt.shared.engine.Engine
import tmt.shared.services.{Command, CommandResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class CommandServiceTest extends org.scalatest.FunSuite with Matchers with ControlDsl {

  val wiring              = new Wiring()
  lazy val engine: Engine = wiring.engine

  def x: CommandResponse = {
    println("blocking")
    CommandResponse("Some Command Response")
  }

  /* Ignoring for time being till we have some asserts here */

  ignore("dd") {
    val dsl = new CommandService(null)
    println(par(x, x))
  }

  ignore("engine pull push") {
    engine.pushAll(
      List(
        Command("setup-assembly1", List(1, 2, 3)),
        Command("setup-assembly2", List(10, 20, 30)),
        Command("setup-assemblies-sequential", List(1, 2, 3, 10, 20, 30)),
        Command("setup-assemblies-parallel", List(1, 2, 3, 10, 20, 30))
      )
    )

    println(engine.hasNext)
    println(engine.pullNext())

    println(engine.hasNext)
    println(engine.pullNext())

    engine.pause()

    Thread.sleep(2000)

    engine.resume()

    println(engine.hasNext)
    println(engine.pullNext())

    println(engine.hasNext)
    println(engine.pullNext())

    engine.pause()
  }
}
