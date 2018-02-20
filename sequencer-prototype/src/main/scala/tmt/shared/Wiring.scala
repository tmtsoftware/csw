package tmt.shared

import akka.typed.{ActorRef, ActorSystem}
import csw.messages.location.Connection
import csw.services.location.scaladsl.LocationService
import tmt.shared.dsl.{ControlDsl, CsDsl, EngineDsl}
import tmt.shared.engine.EngineBehaviour.EngineAction

//TODO: decide better name for wiring
class Wiring(
    system: ActorSystem[Nothing],
    engineActor: ActorRef[EngineAction],
    locationService: LocationService,
    connections: Set[Connection]
) extends ControlDsl {
  lazy val engine: EngineDsl = new EngineDsl(engineActor, system)
  lazy val cs: CsDsl         = new CsDsl(locationService, connections)(system)
  lazy val Command           = tmt.shared.services.Command

  type Command = tmt.shared.services.Command
}
