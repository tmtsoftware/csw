package csw.services.location.commons

import akka.Done
import akka.actor.{ActorRef, ActorSystem, CoordinatedShutdown, Terminated}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.cluster.http.management.ClusterHttpManagement
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
  * CswCluster provides cluster properties to manage distributed data
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
    * Gives handle to CoordinatedShutdown so that shutdown hooks can be added from outside
    */
  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(actorSystem)

  /**
    * Creates an ActorMaterializer for current ActorSystem
    */
  def makeMat(): Materializer = ActorMaterializer()

  /**
    * Terminates the ActorSystem and gracefully leaves the cluster
    *
    * @return A Future that completes on successful shutdown of ActorSystem
    */
  def terminate(): Future[Done] = coordinatedShutdown.run()
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
      //Add shutdown hook if cluster management is started successfully.
      CoordinatedShutdown(actorSystem).addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "shudownClusterManagement") { () =>
        ClusterHttpManagement(cluster).stop()
      }
    }

    // Check if seed nodes are provided to join csw-cluster
    val emptySeeds = actorSystem.settings.config.getStringList("akka.cluster.seed-nodes").isEmpty
    if (emptySeeds) {
      // If no seeds are provided (which happens only during testing), then create a single node cluster by joining to self
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
        Await.result(CoordinatedShutdown(actorSystem).run(), 10.seconds)
        throw ex
    }
  }
}
