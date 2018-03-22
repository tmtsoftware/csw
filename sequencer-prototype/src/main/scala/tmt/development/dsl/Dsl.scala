package tmt.development.dsl

import akka.actor.typed.scaladsl.adapter.{TypedActorSystemOps, UntypedActorSystemOps}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.services.location.commons.ClusterSettings
import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import tmt.shared.Wiring
import tmt.shared.dsl.BaseDsl
import tmt.shared.engine.EngineBehavior
import tmt.shared.engine.EngineBehavior.EngineAction

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object Dsl extends BaseDsl {
  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  lazy val actorSystem: ActorSystem[Nothing] = ClusterSettings().onPort(3552).system.toTyped
  lazy val engineActor: ActorRef[EngineAction] =
    Await.result(actorSystem.systemActorOf(EngineBehavior.behavior, "engine"), timeout.duration)
  lazy val locationService: LocationService = LocationServiceFactory.withSystem(actorSystem.toUntyped)

  lazy val wiring = new Wiring(actorSystem, engineActor, locationService)

  private[tmt] def init(): Unit = {
    actorSystem //touch system so that main does not exit
  }
}
