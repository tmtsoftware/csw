package csw.services.location.scaladsl

import csw.services.location.internal._

/**
  * A `Factory` that manages creation of [[csw.services.location.scaladsl.LocationService]]. ''Note : '' each time the `Factory`
  * creates `LocationService`, a new `ActorSystem` and a Cluster singleton Actor ([[csw.services.location.internal.DeathwatchActor]])
  * will be created
  */
object LocationServiceFactory {

  /**
    * Creates a [[csw.services.location.scaladsl.LocationService]] instance and joins itself in the akka cluster. The data
    * of the akka cluster will now be replicated on this newly created node. ''Note : '' It is highly recommended to create
    * a single instance of `LocationService` and use it throughout.
    *
    * @return A `LocationService` instance
    */
  def make(): LocationService = make(new ActorRuntime())

  /**
    * Creates a [[csw.services.location.scaladsl.LocationService]] instance. It may not join the usual csw akka cluster.
    * ''Note : '' It is highly recommended to use it for testing purposes only.
    *
    * @param actorRuntime An [[csw.services.location.scaladsl.ActorRuntime]] with custom configuration
    * @return A `LocationService` instance
    */
  def make(actorRuntime: ActorRuntime): LocationService = {
    val locationService: LocationService = new LocationServiceImpl(actorRuntime)
    DeathwatchActor.startSingleton(actorRuntime, locationService)
    locationService
  }
}
