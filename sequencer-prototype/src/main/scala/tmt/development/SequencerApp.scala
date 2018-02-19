package tmt.development

import tmt.development.dsl.Dsl._
import tmt.development.dsl.Dsl.wiring._
import tmt.shared.repl.RemoteRepl
import tmt.shared.services.Command

object SequencerApp extends App {

  init()

  RemoteRepl.server.start()

  engine.push(Command("setup-assemblies-parallel", List(1, 2, 3, 10, 20, 30)))

  val params = if (args.isEmpty) Array("scripts/ocs-sequencer.sc") else args
  ammonite.Main.main0(params.toList, System.in, System.out, System.err)

  println("sequencer script loaded and running")
}
