package csw.services.location.javadsl

import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.internal.JLocationServiceImpl
import csw.services.location.scaladsl.LocationServiceFactory

/**
  * The factory is used to create LocationService instance. With each creation, a new ActorSystem will be created and will
  * become part of csw-cluster.
  *
  * ''Note : '' Hence, it is recommended to create a single instance of LocationService and use it throughout the application
  */
object JLocationServiceFactory {

  /**
    * Create a LocationService instance to manage registrations
    */
  def make(): ILocationService = withCluster(CswCluster.make())

  def withSettings(clusterSettings: ClusterSettings): ILocationService = withCluster(CswCluster.withSettings(clusterSettings))

  /**
    * Creates a LocationService instance to manage registrations
    *
    * ''Note : '' It is highly recommended to use it for testing purpose only.
    * @param cswCluster Provide a cluster with customized configuration
    */
  def withCluster(cswCluster: CswCluster): ILocationService = {
    val locationService = LocationServiceFactory.withCluster(cswCluster)
    new JLocationServiceImpl(locationService, cswCluster)
  }
}
