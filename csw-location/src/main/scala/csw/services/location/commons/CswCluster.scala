package csw.services.location.commons

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Terminated}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
  * `CswCluster` manages [[scala.concurrent.ExecutionContext]], [[akka.stream.Materializer]]
  * and `Hostname` of an [[akka.actor.ActorSystem]]
  *
  * @note It is highly recommended that `CswCluster` is created for advanced usages or testing purposes only
  */
class CswCluster private(_actorSystem: ActorSystem) {

  /**
    * Identifies the hostname where `ActorSystem` is running
    */
  val hostname: String = _actorSystem.settings.config.getString("akka.remote.netty.tcp.hostname")

  implicit val actorSystem: ActorSystem = _actorSystem
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer = makeMat()
  implicit val cluster = Cluster(actorSystem)

  /**
    * Replicator of current `ActorSystem`
    */
  val replicator: ActorRef = DistributedData(actorSystem).replicator


  /**
    * Creates an `ActorMaterializer` for current `ActorSystem`
    */
  def makeMat(): Materializer = ActorMaterializer()

  /**
    * Terminates the `ActorSystem` and gracefully disconnects from the cluster.
    *
    * @return A `Future` that completes on `ActorSystem` shutdown
    */
  def terminate(): Future[Done] = CswCluster.terminate(actorSystem)
}

/**
  * Manages initialization and termination of `ActorSystem` and `Cluster`
  */
object CswCluster {
  //do not use the dying actorSystem's dispatcher for scheduling actions after its death.
  import ExecutionContext.Implicits.global

  /**
    * Creates CswCluster with the given settings
    *
    * @see [[CswCluster.withSystem]]
    */
  def make(): CswCluster = withSettings(ClusterSettings())

  /**
    * Creates CswCluster with the given settings
    *
    * @see [[CswCluster.withSystem]]
    */
  def withSettings(settings: ClusterSettings): CswCluster = withSystem(ActorSystem(settings.clusterName, settings.config))

  /**
    * The actorSystem joins csw cluster. If no seed node is provided then the cluster is initialized for `ActorSystem`
    * by self joining.
    *
    * @note The call to this method will be blocked till the actorSystem joins the cluster
    * @return A CswCluster instance that provides extension for actorSystem
    */
  def withSystem(actorSystem: ActorSystem): CswCluster = {
    val cluster = Cluster(actorSystem)
    val emptySeeds = actorSystem.settings.config.getStringList("akka.cluster.seed-nodes").isEmpty
    if (emptySeeds) {
      cluster.join(cluster.selfAddress)
    }
    val p = Promise[Done]
    cluster.registerOnMemberUp(p.success(Done))
    try {
      Await.result(p.future, 10.seconds)
      new CswCluster(actorSystem)
    } catch {
      case NonFatal(ex) ⇒
        Await.result(CswCluster.terminate(actorSystem), 10.seconds)
        throw ex
    }
  }

  /**
    * Performs the termination with two steps in sequence as follows :
    *  - The `ActorSystem` leaves the cluster gracefully and then
    *  - It is terminated
    *
    * @param actorSystem The `ActorSystem` that needs to be cleaned up
    * @return A `Future` that completes on successful shutdown of `ActorSystem`
    */
  def terminate(actorSystem: ActorSystem): Future[Done] = {
    val cluster = Cluster(actorSystem)
    val p = Promise[Terminated]
    cluster.leave(cluster.selfAddress)
    cluster.registerOnMemberRemoved(actorSystem.terminate().onComplete(p.complete))
    p.future.map(_ => Done)
  }
}
