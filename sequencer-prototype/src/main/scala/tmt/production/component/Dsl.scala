package tmt.production.component

import akka.typed.{ActorRef, ActorSystem}
import tmt.shared.dsl.{CommandService, ControlDsl}
import tmt.shared.engine.Engine
import tmt.shared.engine.EngineBehaviour.EngineAction
import tmt.shared.services

object Dsl extends ControlDsl {
  var engine1: Engine = _
  def make(system: ActorSystem[Nothing], engineActor: ActorRef[EngineAction]): Unit = {
    val cs: CommandService = new CommandService(new services.LocationService(system))(system.executionContext) //TODO: fix location service
    engine1 = new Engine(engineActor, system)
  }

  override def engine: Engine = engine1
}
