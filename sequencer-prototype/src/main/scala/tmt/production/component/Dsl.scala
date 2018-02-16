package tmt.production.component

import akka.typed.{ActorRef, ActorSystem}
import tmt.shared.dsl.{CommandService, ControlDsl}
import tmt.shared.engine.EngineDsl
import tmt.shared.engine.EngineBehaviour.EngineAction
import tmt.shared.services

object Dsl extends ControlDsl {
  var engine1: EngineDsl  = _
  var cs1: CommandService = _
  def make(system: ActorSystem[Nothing], engineActor: ActorRef[EngineAction]): Unit = {
    cs1 = new CommandService(new services.LocationService(system))(system.executionContext) //TODO: fix location service
    engine1 = new EngineDsl(engineActor, system)
  }

  lazy val engine: EngineDsl  = engine1
  lazy val cs: CommandService = cs1
  lazy val Command            = tmt.shared.services.Command

  type Command = tmt.shared.services.Command
}
