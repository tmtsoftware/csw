package tmt.sequencer.component

import akka.typed.{ActorRef, ActorSystem}
import tmt.sequencer.dsl.{CommandService, ControlDsl}
import tmt.sequencer.engine.Engine
import tmt.sequencer.engine.EngineBehaviour.EngineAction
import tmt.services

object Dsl extends ControlDsl {
  var engine1: Engine = _
  def make(system: ActorSystem[Nothing], engineActor: ActorRef[EngineAction]): Unit = {
    val cs: CommandService = new CommandService(new services.LocationService(system))(system.executionContext) //TODO: fix location service
    engine1 = new Engine(engineActor, system)
  }

  override def engine: Engine = engine1
}
