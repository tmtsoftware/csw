package csw.services.location.internal

import akka.actor.{ActorRef, Props, Terminated}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{Changed, Subscribe}
import csw.services.location.commons.{CswCluster, LocationServiceLogger}
import csw.services.location.internal.Registry.AllServices
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService
import csw.services.logging._

/**
 * DeathWatchActor tracks the health of all Actors registered with LocationService.
 *
 * @param locationService is used to unregister Actors that are no more alive
 */
class DeathwatchActor(locationService: LocationService) extends LocationServiceLogger.Actor {

  /**
   * List of all actors that DeathwatchActor needs to track
   */
  var watchedLocations: Set[AkkaLocation] = Set.empty

  /**
   * Before starting DeathWatchActor, it is subscribed to replicator to get events for locations registered with LocationService
   *
   * @see [[csw.services.location.internal.Registry.AllServices]]
   */
  override def preStart(): Unit =
    DistributedData(context.system).replicator ! Subscribe(AllServices.Key, self)

  /**
   * Receive function processes two events:
   *
   *  - Changed event will be sent by replicator for newly registered Locations.
   *  - Terminated event will be received upon termination of an actor that was being watched
   *
   * @note DeathwatchActor is primarily interested in Terminated events, but to be able to get those events, it has to track new actor registrations.
   *       So that they can be watched for termination.
   *
   * @see [[akka.cluster.ddata.Replicator.Changed]]
   * @see [[akka.actor.Terminated]]
   */
  override def receive: Receive = {
    case c @ Changed(AllServices.Key) ⇒
      val allLocations = c.get(AllServices.Key).entries.values.toSet
      //take only akka locations
      val akkaLocations = allLocations.collect { case x: AkkaLocation ⇒ x }
      //find out the ones that are not being watched and watch them
      val unwatchedLocations = akkaLocations diff watchedLocations
      unwatchedLocations.foreach(loc ⇒ context.watch(loc.actorRef))
      //all akka locations are now watched
      watchedLocations = akkaLocations
    case Terminated(deadActorRef) =>
      log.info(Map("@msg" → "removing terminated actor", "actorRef" → deadActorRef))
      //stop watching the terminated actor
      context.unwatch(deadActorRef)

      //Unregister the dead akka location and remove it from the list of watched locations
      val maybeLocation = watchedLocations.find(_.actorRef == deadActorRef)
      maybeLocation.foreach { location =>
        locationService.unregister(location.connection)
        watchedLocations -= location
      }
  }
}

object DeathwatchActor {

  /**
   * Create properties for DeathwatchActor
   * @param locationService is used to construct the DeathwatchActor
   */
  def props(locationService: LocationService): Props = Props(new DeathwatchActor(locationService))

  /**
   * Start the DeathwatchActor using the given locationService
   *
   * @param cswCluster is used to get remote ActorSystem to create DeathwatchActor
   */
  def start(cswCluster: CswCluster, locationService: LocationService): ActorRef =
    cswCluster.actorSystem.actorOf(props(locationService), name = "location-service-death-watch-actor")
}
