package csw.location.client.scaladsl
import akka.actor.ActorSystem
import akka.stream.Materializer
import csw.location.api.commons.ClusterAwareSettings
import csw.location.api.exceptions.ClusterSeedsNotFound
import csw.location.api.scaladsl.LocationService
import csw.location.client.internal.LocationServiceClient

/**
 * The factory is used to create LocationService instance.
 */
object HttpLocationServiceFactory {

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
   * Hence clusterSeeds property should be set in the environment variables else [[csw.location.api.exceptions.ClusterSeedsNotFound]] exception will be thrown.
   * */
  def makeRemoteHttpClient(implicit actorSystem: ActorSystem, mat: Materializer): LocationService = {
    val seedIp =
      if (ClusterAwareSettings.seeds.isEmpty) throw ClusterSeedsNotFound
      else ClusterAwareSettings.seeds.head.split(":").head

    new LocationServiceClient(seedIp, httpServerPort)
  }

}
