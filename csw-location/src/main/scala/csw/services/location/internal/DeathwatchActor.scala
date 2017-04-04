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
    * Manages two kinds of events, which are as follows:
    *{{{
    *  - Changed event for `LWWMap`:
    *  The value of `LWWMap` is compared with currently watched akka
    *  locations to find out newly added locations and start death
    *  watching them.
    *
    *  - Terminated event for ActorRef : The `ActorRef`
    *  is unregistered from `LocationService` gracefully and is stopped
    *  being watched.
    *}}}
    */
  override def receive: Receive = {

    case c@Changed(AllServices.Key) ⇒
      val allLocations = c.get(AllServices.Key).entries.values.toSet
      val locations = allLocations.collect { case x: AkkaLocation ⇒ x }
      val addedLocations = locations diff watchedLocations
      addedLocations.foreach(loc ⇒ context.watch(loc.actorRef))
      watchedLocations = locations
    case Terminated(deadActorRef)   =>
      context.unwatch(deadActorRef)
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
