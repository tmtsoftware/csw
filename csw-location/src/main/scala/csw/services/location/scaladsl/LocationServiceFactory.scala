package csw.services.location.scaladsl

import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.internal._

/**
  * The factory is used to create LocationService instance. With each creation, a new ActorSystem will be created and will
  * become part of csw-cluster.
  *
  * @note Hence, it is recommended to create a single instance of LocationService and use it throughout the application
  */
object LocationServiceFactory {

  /**
    * Create a LocationService instance to manage registrations
    */
  def make(): LocationService = withCluster(CswCluster.make())

  /**
    * Create a LocationService to manage registrations
    *
    * @note It is highly recommended to use this method for testing purpose only.
    * @param clusterSettings Provide customized settings for csw-cluster
    */
  def withSettings(clusterSettings: ClusterSettings): LocationService = withCluster(CswCluster.withSettings(clusterSettings))

  /**
    * Creates a LocationService instance to manage registrations
    *
    * @note It is highly recommended to use it for testing purpose only.
    * @param cswCluster Provide a cluster with customized configuration
    */
  def withCluster(cswCluster: CswCluster): LocationService = {
    val locationService: LocationService = new LocationServiceImpl(cswCluster)
    DeathwatchActor.start(cswCluster, locationService)
    locationService
  }
}
