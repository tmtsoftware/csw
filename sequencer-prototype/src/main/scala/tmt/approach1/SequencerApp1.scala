package tmt.approach1

import tmt.sequencer.repl.RemoteRepl
import tmt.services.Command
import tmt.sequencer.dsl.Dsl._

object SequencerApp1 extends App {

  init()

  RemoteRepl.server.start()

  engine.push(Command("setup-assemblies-parallel", List(1, 2, 3, 10, 20, 30)))

  val params = if (args.isEmpty) Array("scripts/ocs-sequencer.sc") else args
  ammonite.Main.main0(params.toList, System.in, System.out, System.err)

  println("sequencer script loaded and running")
}
