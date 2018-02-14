package tmt.sequencer.dsl

import csw.services.location.commons.ClusterSettings
import tmt.sequencer.Wiring
import tmt.sequencer.engine.Engine

object Dsl extends ControlDsl {
  lazy val actorSystem    = ClusterSettings().onPort(3552).system
  private[tmt] val wiring = Wiring.make(actorSystem)
  private[tmt] def init(): Unit = {
    //touch system so that main does not exit
    wiring.system
  }

  lazy val cs: CommandService = wiring.commandService
  lazy val engine: Engine     = wiring.engine

  val Command = tmt.services.Command
  type Command = tmt.services.Command
}
