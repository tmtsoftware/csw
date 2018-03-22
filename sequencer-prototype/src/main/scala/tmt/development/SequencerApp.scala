package tmt.development

import csw.messages.commands.{CommandName, Setup}
import csw.messages.params.models.Prefix
import tmt.development.dsl.Dsl._
import tmt.development.dsl.Dsl.wiring._
import tmt.shared.repl.RemoteRepl

object SequencerApp extends App {

  init()

  RemoteRepl.server.start()

  engine.push(Setup(Prefix(""), CommandName("setup-assemblies-parallel"), None, Set.empty))

  val params = if (args.isEmpty) Array("scripts/ocs-sequencer.sc") else args
  ammonite.Main.main0(params.toList, System.in, System.out, System.err)

  println("sequencer script loaded and running")
}
