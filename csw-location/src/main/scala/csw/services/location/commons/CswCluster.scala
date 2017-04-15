package csw.services.location.commons

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Terminated}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.cluster.http.management.ClusterHttpManagement
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
  * CswCluster provides cluster properties to manage data in CRDT
  *
  * ''Note: '' It is highly recommended that explicit creation of CswCluster should be for advanced usages or testing purposes only
  */
class CswCluster private(_actorSystem: ActorSystem) {

  /**
    * Identifies the hostname where ActorSystem is running
    */
  val hostname: String = _actorSystem.settings.config.getString("akka.remote.netty.tcp.hostname")

  implicit val actorSystem: ActorSystem = _actorSystem
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer = makeMat()
  implicit val cluster = Cluster(actorSystem)

  /**
    * Gives the replicator for the current ActorSystem
    *
    * @see [[akka.cluster.ddata.Replicator]]
    */
  val replicator: ActorRef = DistributedData(actorSystem).replicator


  /**
    * Creates an ActorMaterializer for current ActorSystem
    */
  def makeMat(): Materializer = ActorMaterializer()

  /**
    * Terminates the ActorSystem and gracefully leaves the cluster
    *
    * @return A Future that completes on successful shutdown of ActorSystem
    */
  def terminate(): Future[Done] = CswCluster.terminate(actorSystem)
}

/**
  * Manages initialization and termination of ActorSystem and the Cluster.
  *
  * ''Note: '' The creation of CswCluster will be blocked till the ActorSystem joins csw-cluster successfully
  */
object CswCluster {
  //do not use the dying actorSystem's dispatcher for scheduling actions after its death.
  import ExecutionContext.Implicits.global

  /**
    * Creates CswCluster with the default cluster settings
    *
    * @see [[csw.services.location.commons.ClusterSettings]]
    */
  def make(): CswCluster = withSettings(ClusterSettings())

  /**
    * Creates CswCluster with the given customized settings
    */
  def withSettings(settings: ClusterSettings): CswCluster = withSystem(ActorSystem(settings.clusterName, settings.config))


  /**
    * Creates CswCluster with the given ActorSystem
    */
  def withSystem(actorSystem: ActorSystem): CswCluster = {
    // Get the cluster information of this ActorSystem
    val cluster: Cluster = Cluster(actorSystem)

    // Get the startManagement flag for the ActorSystem
    val startManagement = actorSystem.settings.config.getBoolean("startManagement")
    if(startManagement) {
      //Block until the cluster is initialized
      Await.result(ClusterHttpManagement(cluster).start(), 10.seconds)
    }

    // Check if seed nodes are provided to join csw-cluster
    val emptySeeds = actorSystem.settings.config.getStringList("akka.cluster.seed-nodes").isEmpty
    if (emptySeeds) {
      // Join the cluster on self node. It will be mostly used for testing.
      cluster.join(cluster.selfAddress)
    }

    val p = Promise[Done]
    // Once the current ActorSystem has joined csw-cluster, the promise will be completed
    cluster.registerOnMemberUp(p.success(Done))

    try {
      // Block until the ActorSystem joins csw-cluster successfully
      Await.result(p.future, 20.seconds)
      // return the CswCluster instance with this ActorSystem
      new CswCluster(actorSystem)
    } catch {
      case NonFatal(ex) ⇒
        Await.result(ClusterHttpManagement(cluster).stop(), 10.seconds)
        Await.result(CswCluster.terminate(actorSystem), 10.seconds)
        throw ex
    }
  }

  /**
    * Performs the termination as follows :
    *  - The given ActorSystem is requested to leave the cluster gracefully
    *  - and once it has left the cluster, it is terminated
    *
    * @param actorSystem The ActorSystem that needs to be cleaned up
    * @return A Future that completes on successful shutdown of ActorSystem
    */
  def terminate(actorSystem: ActorSystem): Future[Done] = {
    // get the cluster information of this ActorSystem
    val cluster = Cluster(actorSystem)
    val p = Promise[Terminated]
    // request to leave the self node from the cluster
    cluster.leave(cluster.selfAddress)
    // once the self node has gracefully left the cluster, request to terminate the ActorSystem
    cluster.registerOnMemberRemoved(actorSystem.terminate().onComplete(p.complete))
    // The promise will be completed once the ActorSystem has successfully shutdown
    p.future.map(_ => Done)
  }
}
