package tmt.sequencer

import scala.language.implicitConversions

object Dsl extends ControlDsl {
  private[tmt] val wiring = new Wiring
  private[tmt] def init(): Unit = {
    //touch system so that main does not exit
    wiring.system
  }

  lazy val cs: CommandService = wiring.commandService
  lazy val engine: Engine     = wiring.engine

  val Command = tmt.services.Command
  type Command = tmt.services.Command
}
