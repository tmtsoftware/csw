package csw.services.location.internal

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{Changed, Subscribe}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import csw.services.location.internal.Registry.AllServices
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorRuntime, LocationService}

/**
  * An `Actor` that death watches all registered `ActorRefs` in csw akka cluster and subscribes for changes in `LWWMap` data.
  * As soon as there are changes detected in `LWWMap`, it looks for previously unwatched `ActorRefs` and starts death watching it.
  *
  * @param locationService The `LocationService` is used for un-registering `ActorRef` for which `Terminated` message is received
  */
class DeathwatchActor(locationService: LocationService) extends Actor {

  var watchedLocations: Set[AkkaLocation] = Set.empty

  /**
    * Subscribes for changes in `LWWMap` data before starting this `Actor`
    */
  override def preStart(): Unit = {
    DistributedData(context.system).replicator ! Subscribe(AllServices.Key, self)
  }

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

  /**
    * Starts a [[csw.services.location.internal.DeathwatchActor]] as Cluster singleton
    *
    * @param actorRuntime    The `ActorSystem` from `ActorRuntime` is used to create `DeathwatchActor`
    * @param locationService `LocationService` instance needed for `DeathwatchActor` creation
    */
  def startSingleton(actorRuntime: ActorRuntime, locationService: LocationService): ActorRef = {
    import actorRuntime._
    actorSystem.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(new DeathwatchActor(locationService)),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(actorSystem)
      ),
      name = "location-service-death-watch-actor"
    )
  }

}
