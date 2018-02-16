package tmt.development.dsl

import csw.services.location.commons.ClusterSettings
import tmt.development.Wiring
import tmt.shared.dsl.{CommandService, ControlDsl}
import tmt.shared.engine.EngineDsl

object Dsl extends ControlDsl {
  lazy val actorSystem    = ClusterSettings().onPort(3552).system
  private[tmt] val wiring = Wiring.make(actorSystem)
  private[tmt] def init(): Unit = {
    //touch system so that main does not exit
    wiring.system
  }

  lazy val cs: CommandService = wiring.commandService
  lazy val engine: EngineDsl  = wiring.engine

  val Command = tmt.shared.services.Command
  type Command = tmt.shared.services.Command
}
