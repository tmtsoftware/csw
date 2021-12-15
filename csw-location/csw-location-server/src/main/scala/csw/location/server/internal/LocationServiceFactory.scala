package csw.location.server.internal

import csw.location.api.CswVersionJvm
import csw.location.api.scaladsl.LocationService
import csw.location.server.commons.{ClusterSettings, CswCluster}

/**
 * The factory is used to create LocationService instance. With each creation, a new ActorSystem will be created and will
 * become part of csw-cluster. Currently, the LocationService instance is created in `csw-framework`.
 *
 * @note hence, it is recommended to create a single instance of LocationService and use it throughout the application
 */
private[location] object LocationServiceFactory {

  /**
   * Create an LocationService instance to manage registrations
   *
   * @param clusterSettings instance of cluster settings which contains all the cluster related configuration
   * @return an instance of `LocationService`
   */
  def make(clusterSettings: ClusterSettings): LocationService = {
    val cswCluster                       = CswCluster.make(clusterSettings)
    val locationService: LocationService = new LocationServiceImpl(cswCluster)
    // starts a DeathwatchActor each time a LocationService is created
    DeathwatchActor.start(cswCluster, locationService)
    locationService
  }

}
