package csw.services.location.internal

import akka.cluster.ddata.Replicator.{Changed, Subscribe}
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior, Terminated}
import csw.messages.location.{AkkaLocation, HttpLocation, Location, TcpLocation}
import csw.services.location.commons.{CswCluster, LocationServiceLogger}
import csw.services.location.internal.Registry.AllServices
import csw.services.location.scaladsl.LocationService

/**
 * DeathWatchActor tracks the health of all Actors registered with LocationService.
 *
 * @param locationService is used to unregister Actors that are no more alive
 */
class DeathwatchActor(locationService: LocationService) extends LocationServiceLogger.Simple {
  import DeathwatchActor.Msg

  /**
   * Deathwatch behavior processes `DeathwatchActor.Msg` type events sent by replicator for newly registered Locations.
   * Terminated signal will be received upon termination of an actor that was being watched.
   *
   * @see [[akka.actor.Terminated]]
   */
  def behavior(watchedLocations: Set[Location]): Behavior[Msg] =
    Actor.immutable[Msg] { (context, changeMsg) ⇒
      val allLocations = changeMsg.get(AllServices.Key).entries.values.toSet

      //find out the ones that are not being watched and watch them
      val unwatchedLocations = allLocations diff watchedLocations

      //Multiple AkkaLocation can have same logAdminActorRef, hence watch actorRef instead of logAdminActorRef for akka
      unwatchedLocations.foreach(loc ⇒ {
        val actorRefToWatch = loc match {
          case AkkaLocation(_, _, _, actorRef, _)       ⇒ actorRef
          case loc @ (_: HttpLocation | _: TcpLocation) ⇒ loc.logAdminActorRef
        }
        log.debug(s"Started watching actor: ${actorRefToWatch.toString}")
        context.watch(actorRefToWatch)
      })
      //all locations are now watched
      behavior(allLocations)
    } onSignal {
      case (ctx, Terminated(deadActorRef)) ⇒
        log.warn(s"Un-watching terminated actor: ${deadActorRef.toString}")
        //stop watching the terminated actor
        ctx.unwatch(deadActorRef)
        //Unregister the dead location and remove it from the list of watched locations
        val maybeLocation = watchedLocations.find {
          case AkkaLocation(_, _, _, actorRef, _)       ⇒ deadActorRef == actorRef
          case loc @ (_: HttpLocation | _: TcpLocation) ⇒ deadActorRef == loc.logAdminActorRef
        }
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
  import akka.typed.scaladsl.adapter._
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
