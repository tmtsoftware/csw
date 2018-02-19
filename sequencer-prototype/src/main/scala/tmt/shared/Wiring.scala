package tmt.shared

import akka.typed.{ActorRef, ActorSystem}
import tmt.shared.dsl.{ControlDsl, CsDsl, EngineDsl}
import tmt.shared.engine.EngineBehaviour.EngineAction

class Wiring(system: ActorSystem[Nothing], engineActor: ActorRef[EngineAction]) extends ControlDsl {
  lazy val engine: EngineDsl = new EngineDsl(engineActor, system)
  lazy val cs: CsDsl         = new CsDsl(new services.LocationService(system))(system.executionContext) //TODO: fix location service
  lazy val Command           = tmt.shared.services.Command

  type Command = tmt.shared.services.Command
}
