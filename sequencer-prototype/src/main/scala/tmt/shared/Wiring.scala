package tmt.shared

import akka.actor.typed.{ActorRef, ActorSystem}
import csw.services.location.scaladsl.LocationService
import tmt.shared.dsl.{ControlDsl, CswServicesDsl, EngineDsl}
import tmt.shared.engine.EngineBehavior.EngineAction

//TODO: decide better name for wiring
class Wiring(
    system: ActorSystem[Nothing],
    engineActor: ActorRef[EngineAction],
    locationService: LocationService
) extends ControlDsl {
  lazy val engine: EngineDsl  = new EngineDsl(engineActor, system)
  lazy val cs: CswServicesDsl = new CswServicesDsl(locationService)(system)
  lazy val Command            = tmt.shared.services.Command

  type Command = tmt.shared.services.Command
}
