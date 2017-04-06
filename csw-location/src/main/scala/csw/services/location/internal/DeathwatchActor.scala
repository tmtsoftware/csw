package csw.services.location.internal

import akka.actor.{Actor, ActorRef, Props, Terminated}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{Changed, Subscribe}
import csw.services.location.commons.CswCluster
import csw.services.location.internal.Registry.AllServices
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

/**
  * An `Actor` that death watches all registered `ActorRefs` in csw akka cluster and subscribes for changes in `LWWMap` data.
  * As soon as there are changes detected in `LWWMap`, it looks for previously unwatched `ActorRefs` and starts death watching it.
  *
  * @param locationService The `LocationService` is used for un-registering `ActorRef` for which `Terminated` message is received
  */
class DeathwatchActor(locationService: LocationService) extends Actor {

  var watchedLocations: Set[AkkaLocation] = Set.empty

  /**
    * Subscribes for changes in `LWWMap` data before starting this `Actor`. Current state for `LWWMap` will given to DeathwatchActor
    * through [[akka.cluster.ddata.Replicator.Changed]] message
    */
  override def preStart(): Unit = {
    DistributedData(context.system).replicator ! Subscribe(AllServices.Key, self)
  }

  /**
    * Manages `Changed` event for `LWWMap` and `Terminated` event for `ActorRef`
    */
  override def receive: Receive = {
    case c@Changed(AllServices.Key) ⇒
      val allLocations = c.get(AllServices.Key).entries.values.toSet
      //take only akka locations
      val akkaLocations = allLocations.collect { case x: AkkaLocation ⇒ x }
      //find out the ones that are not being watched and watch them
      val unwatchedLocations = akkaLocations diff watchedLocations
      unwatchedLocations.foreach(loc ⇒ context.watch(loc.actorRef))
      //all akka locations are now watched
      watchedLocations = akkaLocations
    case Terminated(deadActorRef)   =>
      //unwatch the terminated akka location
      context.unwatch(deadActorRef)

      //Unregister the terminated akka location and remove it from watched locations
      val maybeLocation = watchedLocations.find(_.actorRef == deadActorRef)
      maybeLocation.foreach { location =>
        locationService.unregister(location.connection)
        watchedLocations -= location
      }
  }
}

object DeathwatchActor {

  def props(locationService: LocationService): Props = Props(new DeathwatchActor(locationService))

  /**
    * Starts a [[csw.services.location.internal.DeathwatchActor]]
    *
    * @param cswCluster    The `ActorSystem` from `CswCluster` is used to create `DeathwatchActor`
    * @param locationService `LocationService` instance needed for `DeathwatchActor` creation
    */
  def start(cswCluster: CswCluster, locationService: LocationService): ActorRef = {
    cswCluster.actorSystem.actorOf(props(locationService),
      name = "location-service-death-watch-actor"
    )
  }
}