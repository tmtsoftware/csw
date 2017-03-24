package csw.services.location.internal

import akka.actor.{Actor, Props, Terminated}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{Changed, Subscribe}
import csw.services.location.models._
import csw.services.location.scaladsl.LocationService

class DeathwatchActor(locationService: LocationService) extends Actor {

  var watchedLocations: Set[AkkaLocation] = Set.empty

  override def preStart(): Unit = {
    DistributedData(context.system).replicator ! Subscribe(Constants.RegistryKey, self)
  }

  override def receive: Receive = {
    case c@Changed(Constants.RegistryKey) ⇒
      val allLocations = c.get(Constants.RegistryKey).entries.values.toSet
      val locations = allLocations.collect { case x: AkkaLocation ⇒ x }
      val addedLocations = locations diff watchedLocations
      addedLocations.foreach(loc ⇒ context.watch(loc.actorRef))
      watchedLocations = locations
    case Terminated(deadActorRef)         =>
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
}
