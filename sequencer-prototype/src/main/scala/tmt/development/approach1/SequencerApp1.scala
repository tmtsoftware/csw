package tmt.development.approach1

import java.io.File

import tmt.shared.repl.RemoteRepl
import tmt.shared.services.Command
import tmt.development.dsl.Dsl._

object SequencerApp1 extends App {

  init()

  RemoteRepl.server.start()

  engine.push(Command("setup-assemblies-parallel", List(1, 2, 3, 10, 20, 30)))

  val params = if (args.isEmpty) Array("scripts/ocs-sequencer.sc") else args
  ammonite.Main.main0(params.toList, System.in, System.out, System.err)

  println("sequencer script loaded and running")
}
