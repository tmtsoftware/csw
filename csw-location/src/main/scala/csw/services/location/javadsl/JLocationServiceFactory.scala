package csw.services.location.javadsl

import csw.services.location.internal.{JLocationServiceImpl, Settings}
import csw.services.location.scaladsl.{CswCluster, LocationService, LocationServiceFactory}

/**
  * A `Factory` that manages creation of [[csw.services.location.javadsl.ILocationService]].
  *
  * ''Note : '' Each time the `Factory` creates `ILocationService`, a new `ActorSystem` and a Cluster singleton Actor
  *       ([[csw.services.location.internal.DeathwatchActor]]) will be created
  */
object JLocationServiceFactory {

  /**
    * Creates a [[csw.services.location.javadsl.ILocationService]] instance and joins itself to the akka cluster. The data
    * of the akka cluster will now be replicated on this newly created node. An `ILocationService` instance is returned.
    *
    * ''Note : '' It is recommended to create a single instance of `ILocationService` and use it throughout.
    */
  def make(): ILocationService = withCluster(CswCluster.make())

  /**
    * Creates a [[csw.services.location.javadsl.ILocationService]] instance. An `ILocationService` instance.
    * ''Note : '' It is highly recommended to use it for testing purposes only.
    *
    * @param cswCluster An [[csw.services.location.scaladsl.CswCluster]] with custom configuration
    */
  def withCluster(cswCluster: CswCluster): ILocationService = {
    val locationService: LocationService = LocationServiceFactory.withCluster(cswCluster)
    new JLocationServiceImpl(locationService, cswCluster)
  }
}
