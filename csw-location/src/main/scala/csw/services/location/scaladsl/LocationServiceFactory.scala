package csw.services.location.scaladsl

import akka.actor.ActorSystem
import csw.services.location.commons.{ClusterSettings, CswCluster, LocationServiceLogger}
import csw.services.location.internal._

/**
 * The factory is used to create LocationService instance. With each creation, a new ActorSystem will be created and will
 * become part of csw-cluster.
 *
 * @note Hence, it is recommended to create a single instance of LocationService and use it throughout the application
 */
object LocationServiceFactory extends LocationServiceLogger.Simple {

  /**
   * Create a LocationService instance to manage registrations
   */
  def make(): LocationService = withCluster(CswCluster.make())

  def withSystem(actorSystem: ActorSystem): LocationService =
    withCluster(CswCluster.withSystem(actorSystem))

  /**
   * Create a LocationService instance to manage registrations
   *
   * @note It is highly recommended to use this method for testing purpose only.
   */
  def withSettings(clusterSettings: ClusterSettings): LocationService =
    withCluster(CswCluster.withSettings(clusterSettings))

  /**
   * Create a LocationService instance to manage registrations
   *
   * @note It is highly recommended to use it for testing purpose only.
   */
  def withCluster(cswCluster: CswCluster): LocationService = {
    val locationService: LocationService = new LocationServiceImpl(cswCluster)
    // starts a DeathwatchActor each time a LocationService is created
    DeathwatchActor.start(cswCluster, locationService)
    locationService
  }
}
