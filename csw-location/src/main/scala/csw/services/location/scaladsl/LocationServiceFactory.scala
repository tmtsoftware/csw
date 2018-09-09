package csw.services.location.scaladsl

import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.services.location.commons.{ClusterAwareSettings, ClusterSettings, CswCluster}
import csw.messages.location.exceptions.ClusterSeedsNotFound
import csw.messages.location.scaladsl.LocationService
import csw.services.location.internal._

/**
 * The factory is used to create LocationService instance. With each creation, a new ActorSystem will be created and will
 * become part of csw-cluster. Currently, the LocationService instance is created in `csw-framework`.
 *
 * @note hence, it is recommended to create a single instance of LocationService and use it throughout the application
 */
object LocationServiceFactory {

  /**
   * Create a LocationService instance to manage registrations
   *
   * @throws csw.messages.location.exceptions.CouldNotEnsureDataReplication
   * @throws csw.messages.location.exceptions.CouldNotJoinCluster
   * @return an instance of `LocationService`
   */
  def make(): LocationService = withCluster(CswCluster.make())

  /**
   * Create an LocationService instance to manage registrations
   *
   * @param actorSystem the actorSystem used to feed in `CswCluster` and use it's config properties to join the cluster
   * @throws csw.messages.location.exceptions.CouldNotEnsureDataReplication
   * @throws csw.messages.location.exceptions.CouldNotJoinCluster
   * @return an instance of `LocationService`
   */
  def withSystem(actorSystem: ActorSystem): LocationService =
    withCluster(CswCluster.withSystem(actorSystem))

  /**
   * Create an LocationService instance to manage registrations
   *
   * @note it is highly recommended to use this method for testing purpose only
   * @param clusterSettings the custom clusterSettings used to join the cluster
   * @throws csw.messages.location.exceptions.CouldNotEnsureDataReplication
   * @throws csw.messages.location.exceptions.CouldNotJoinCluster
   * @return an instance of `LocationService`
   */
  def withSettings(clusterSettings: ClusterSettings): LocationService =
    withCluster(CswCluster.withSettings(clusterSettings))

  /**
   * Create a LocationService instance to manage registrations
   *
   * @note it is highly recommended to use it for testing purpose only
   * @param cswCluster the cswCluster instance used to join the cluster
   * @throws csw.messages.location.exceptions.CouldNotEnsureDataReplication
   * @throws csw.messages.location.exceptions.CouldNotJoinCluster
   * @return an instance of `LocationService`
   */
  def withCluster(cswCluster: CswCluster): LocationService = {
    val locationService: LocationService = new LocationServiceImpl(cswCluster)
    // starts a DeathwatchActor each time a LocationService is created
    DeathwatchActor.start(cswCluster, locationService)
    locationService
  }

  private val httpServerPort = 7654

  /**
   * Use this factory method to create http location client when cluster seed is running locally.
   * Cluster seed starts location http server on port 7654.
   * Short running command line applications can use this factory method to get http access to location service,
   * so that they do not need to join and leave akka cluster.
   * */
  def makeLocalHttpClient(implicit actorSystem: ActorSystem, mat: Materializer): LocationService =
    new LocationServiceClient("localhost", httpServerPort)

  /**
   * Use this factory method to create http location client when cluster seed is running remotely.
   * Cluster seed starts location http server on port 7654.
   * This client tries to connect to the location server running on first cluster seed node.
   * Hence clusterSeeds property should be set in the environment variables else [[ClusterSeedsNotFound]] exception will be thrown.
   * */
  def makeRemoteHttpClient(implicit actorSystem: ActorSystem, mat: Materializer): LocationService = {
    val seedIp =
      if (ClusterAwareSettings.seeds.isEmpty) throw ClusterSeedsNotFound
      else ClusterAwareSettings.seeds.head.split(":").head

    new LocationServiceClient(seedIp, httpServerPort)
  }
}
