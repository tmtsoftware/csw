package csw.services.location.commons

import akka.Done
import akka.actor.{ActorRef, ActorSystem, CoordinatedShutdown}
import akka.cluster.{Cluster, MemberStatus}
import akka.cluster.ddata.DistributedData
import akka.cluster.ddata.Replicator.{GetReplicaCount, ReplicaCount}
import akka.cluster.http.management.ClusterHttpManagement
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
 * CswCluster provides cluster properties to manage distributed data
 *
 * ''Note: '' It is highly recommended that explicit creation of CswCluster should be for advanced usages or testing purposes only
 */
class CswCluster private (_actorSystem: ActorSystem) {

  /**
   * Identifies the hostname where ActorSystem is running
   */
  val hostname: String = _actorSystem.settings.config.getString("akka.remote.netty.tcp.hostname")

  implicit val actorSystem: ActorSystem = _actorSystem
  implicit val ec: ExecutionContext     = actorSystem.dispatcher
  implicit val mat: Materializer        = makeMat()
  implicit val cluster                  = Cluster(actorSystem)

  /**
   * Gives the replicator for the current ActorSystem
   */
  val replicator: ActorRef = DistributedData(actorSystem).replicator

  /**
   * Gives handle to CoordinatedShutdown extension
   */
  private val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(actorSystem)

  /**
   * Creates an ActorMaterializer for current ActorSystem
   */
  def makeMat(): Materializer = ActorMaterializer()

  def addJvmShutdownHook[T](hook: ⇒ T): Unit = coordinatedShutdown.addJvmShutdownHook(hook)

  /**
   * aaa
   */
  private def startClusterManagement(): Unit = {
    val startManagement = actorSystem.settings.config.getBoolean("startManagement")
    if (startManagement) {
      val clusterHttpManagement = ClusterHttpManagement(cluster)
      //Add shutdown hook if cluster management is started successfully.
      coordinatedShutdown.addTask(CoordinatedShutdown.PhaseBeforeServiceUnbind, "shutdownClusterManagement") { () =>
        clusterHttpManagement.stop()
      }
      Await.result(clusterHttpManagement.start(), 10.seconds)
    }
  }

  private def joinCluster(): Done = {
    // Check if seed nodes are provided to join csw-cluster
    val emptySeeds = actorSystem.settings.config.getStringList("akka.cluster.seed-nodes").isEmpty
    if (emptySeeds) {
      // If no seeds are provided (which happens only during testing), then create a single node cluster by joining to self
      cluster.join(cluster.selfAddress)
    }

    val p = Promise[Done]
    // Once the current ActorSystem has joined csw-cluster, the promise will be completed
    cluster.registerOnMemberUp(p.success(Done))
    Await.result(p.future, 20.seconds)
  }

  private def ensureReplication(): Unit = {
    implicit val timeout = Timeout(5.seconds)
    import akka.pattern.ask
    def replicaCountF = (replicator ? GetReplicaCount).mapTo[ReplicaCount]
    def replicaCount  = Await.result(replicaCountF, 5.seconds).n
    def upMembers     = cluster.state.members.count(_.status == MemberStatus.Up)
    BlockingUtils.awaitAssert(replicaCount == upMembers)
  }

  /**
   * Terminates the ActorSystem and gracefully leaves the cluster
   */
  def shutdown(): Future[Done] = coordinatedShutdown.run()
}

/**
 * Manages initialization and termination of ActorSystem and the Cluster.
 *
 * ''Note: '' The creation of CswCluster will be blocked till the ActorSystem joins csw-cluster successfully
 */
object CswCluster {
  //do not use the dying actorSystem's dispatcher for scheduling actions after its death.

  /**
   * Creates CswCluster with the default cluster settings
   *
   * @see [[csw.services.location.commons.ClusterSettings]]
   */
  def make(): CswCluster = withSettings(ClusterSettings())

  /**
   * Creates CswCluster with the given customized settings
   */
  def withSettings(settings: ClusterSettings): CswCluster =
    withSystem(ActorSystem(settings.clusterName, settings.config))

  /**
   * Creates CswCluster with the given ActorSystem
   */
  def withSystem(actorSystem: ActorSystem): CswCluster = {
    val cswCluster = new CswCluster(actorSystem)
    try {
      cswCluster.startClusterManagement()
      cswCluster.joinCluster()
      cswCluster.ensureReplication()
      cswCluster
    } catch {
      case NonFatal(ex) ⇒
        Await.result(cswCluster.shutdown(), 10.seconds)
        throw ex
    }
  }
}
