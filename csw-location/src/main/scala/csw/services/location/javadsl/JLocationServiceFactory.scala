package csw.services.location.javadsl

import csw.services.location.commons.{ClusterSettings, CswCluster}
import csw.services.location.internal.JLocationServiceImpl
import csw.services.location.scaladsl.LocationServiceFactory

/**
 * The factory is used to create ILocationService instance. With each creation, a new ActorSystem will be created and will
 * become the part of csw-cluster. Currently, the ILocationService instance is created in `csw-framework`.
 *
 * @note Hence, it is recommended to create a single instance of ILocationService and use it throughout the application
 */
object JLocationServiceFactory {

  /**
   * Create an ILocationService instance to manage registrations
   *
   * @return An instance of `ILocationService`
   */
  def make(): ILocationService = withCluster(CswCluster.make())

  /**
   * Create an ILocationService instance to manage registrations
   *
   * @note It is highly recommended to use this method for testing purpose only
   * @param clusterSettings The custom clusterSettings used to join the cluster
   * @return An instance of `ILocationService`
   */
  def withSettings(clusterSettings: ClusterSettings): ILocationService =
    withCluster(CswCluster.withSettings(clusterSettings))

  /**
   * Create an ILocationService instance to manage registrations
   *
   * @note It is highly recommended to use it for testing purpose only.
   * @param cswCluster The cswCluster instance used to join the cluster
   * @return An instance of `ILocationService`
   */
  def withCluster(cswCluster: CswCluster): ILocationService = {
    val locationService = LocationServiceFactory.withCluster(cswCluster)
    new JLocationServiceImpl(locationService, cswCluster)
  }
}
