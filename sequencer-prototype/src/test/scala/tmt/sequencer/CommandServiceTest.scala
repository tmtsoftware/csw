package tmt.sequencer

import org.scalatest.Matchers
import Dsl._
import tmt.services.CommandResponse

import scala.concurrent.ExecutionContext.Implicits.global

class CommandServiceTest extends org.scalatest.FunSuite with Matchers {

  def x: CommandResponse = {
    println("blocking")
    CommandResponse("Some Command Response")
  }

  test("dd") {
    val dsl = new CommandService(null)
    println(par(x, x))
  }

  test("engine pull push") {
    val wiring = new Wiring()
    import wiring.engine
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

    engine.push(Command("setup-assembly1", List(1, 2, 3)))

    engine.hasNext
    engine.pullNext()

    Thread.sleep(5000)
    engine.resume()
    Thread.sleep(1000)
  }
}
