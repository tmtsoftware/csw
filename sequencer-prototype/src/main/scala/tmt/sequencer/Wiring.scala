package tmt.sequencer

import akka.actor.ActorSystem
import akka.typed
import akka.typed.ActorRef
import akka.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.util.Timeout
import tmt.sequencer.dsl.CommandService
import tmt.sequencer.engine.EngineBehaviour.EngineAction
import tmt.sequencer.engine.{Engine, EngineBehaviour}
import tmt.services.LocationService

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class Wiring {
  implicit lazy val timeout: Timeout          = Timeout(5.seconds)
  lazy val system: typed.ActorSystem[Nothing] = ActorSystem("test").toTyped
  lazy val engineActor: ActorRef[EngineAction] =
    Await.result(system.systemActorOf(EngineBehaviour.behaviour, "engine"), timeout.duration)
  lazy val engine = new Engine(engineActor, system)

  lazy val locationService = new LocationService(system)
  lazy val commandService  = new CommandService(locationService)(system.executionContext)
}

object Wiring {
  def make(_actorSystem: ActorSystem) = new Wiring {
    override lazy val system: typed.ActorSystem[Nothing] = _actorSystem.toTyped
  }
}
