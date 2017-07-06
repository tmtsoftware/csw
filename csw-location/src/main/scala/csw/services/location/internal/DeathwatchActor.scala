package csw.services.location.internal

import akka.cluster.ddata.Replicator.{Changed, Subscribe}
import akka.typed.scaladsl.Actor
import akka.typed.scaladsl.adapter._
import akka.typed.{ActorRef, Behavior, Terminated}
import csw.services.location.commons.{CswCluster, LocationServiceLogger}
import csw.services.location.internal.Registry.AllServices
import csw.services.location.models.AkkaLocation
import csw.services.location.scaladsl.LocationService

/**
 * DeathWatchActor tracks the health of all Actors registered with LocationService.
 *
 * @param locationService is used to unregister Actors that are no more alive
 */
class DeathwatchActor(locationService: LocationService) extends LocationServiceLogger.Simple {
  import DeathwatchActor.Msg

  /**
   * Deathwatch behaviour processes `DeathwatchActor.Msg` type events sent by replicator for newly registered Locations.
   * Terminated signal will be received upon termination of an actor that was being watched.
   *
   * @see [[akka.actor.Terminated]]
   */
  def behavior(watchedLocations: Set[AkkaLocation]): Behavior[Msg] =
    Actor.immutable[Msg] { (context, changeMsg) ⇒
      val allLocations = changeMsg.get(AllServices.Key).entries.values.toSet
      //take only akka locations
      val akkaLocations = allLocations.collect { case x: AkkaLocation ⇒ x }
      //find out the ones that are not being watched and watch them
      val unwatchedLocations = akkaLocations diff watchedLocations
      unwatchedLocations.foreach(loc ⇒ {
        log.debug(s"Started watching actor", Map("actorRef" → loc.actorRef.toString()))
        context.watch(loc.actorRef)
      })
      //all akka locations are now watched
      behavior(akkaLocations)
    } onSignal {
      case (ctx, Terminated(deadActorRef)) ⇒
        log.info("Removing terminated actor", Map("actorRef" → deadActorRef.toString))
        //stop watching the terminated actor
        ctx.unwatch(deadActorRef)
        //Unregister the dead akka location and remove it from the list of watched locations
        val maybeLocation = watchedLocations.find(_.actorRef == deadActorRef.toUntyped)
        maybeLocation match {
          case Some(location) =>
            //if deadActorRef is mapped to a location, unregister it and remove it from watched locations
            locationService.unregister(location.connection)
            behavior(watchedLocations - location)
          case None ⇒
            //if deadActorRef does not match any location, don't change a thing!
            Actor.same
        }
    }
}

object DeathwatchActor extends LocationServiceLogger.Simple {
  //message type handled by the for the typed deathwatch actor
  type Msg = Changed[AllServices.Value]

  /**
   * Start the DeathwatchActor using the given locationService
   *
   * @param cswCluster is used to get remote ActorSystem to create DeathwatchActor
   */
  def start(cswCluster: CswCluster, locationService: LocationService): ActorRef[Msg] = {
    log.debug("Starting Deathwatch actor")
    val actorRef = cswCluster.actorSystem.spawn(
      //span the actor with empty set of watched locations
      new DeathwatchActor(locationService).behavior(Set.empty),
      name = "location-service-death-watch-actor"
    )

    //Subscribed to replicator to get events for locations registered with LocationService
    cswCluster.replicator ! Subscribe(AllServices.Key, actorRef.toUntyped)
    actorRef
  }
}
