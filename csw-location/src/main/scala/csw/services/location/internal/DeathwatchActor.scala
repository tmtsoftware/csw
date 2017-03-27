package csw.services.location.internal

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Terminated}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{Changed, Subscribe}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import csw.services.location.internal.Registry.AllServices
import csw.services.location.models._
import csw.services.location.scaladsl.{ActorRuntime, LocationService}

class DeathwatchActor(locationService: LocationService) extends Actor {

  var watchedLocations: Set[AkkaLocation] = Set.empty

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
