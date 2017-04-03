package csw.services.location.scaladsl

import csw.services.location.commons.CswCluster
import csw.services.location.internal._

/**
  * A `Factory` that manages creation of [[csw.services.location.scaladsl.LocationService]].
  *
  * @note Each time the `Factory` creates `LocationService`, a new `ActorSystem` and a Cluster singleton Actor
  *       ([[csw.services.location.internal.DeathwatchActor]]) will be created
  */
object LocationServiceFactory {

  /**
    * Creates a [[csw.services.location.scaladsl.LocationService]] instance and joins to the akka cluster. The data
    * of the akka cluster will now be replicated on this newly created node.
    *
    * @note It is recommended to create a single instance of `LocationService` and use it throughout.
    * @return A `LocationService` instance
    */
  def make(): LocationService = withCluster(CswCluster.make())

  /**
    * Creates a [[csw.services.location.scaladsl.LocationService]] instance. The data of the akka cluster will now be replicated
    * on this newly created node.
    *
    * @note It is highly recommended to use it for testing purposes only.
    * @param cswCluster An [[CswCluster]] with custom configuration
    * @return A `LocationService` instance
    */
  def withCluster(cswCluster: CswCluster): LocationService = {
    val locationService: LocationService = new LocationServiceImpl(cswCluster)
    DeathwatchActor.startSingleton(cswCluster, locationService)
    locationService
  }
}
