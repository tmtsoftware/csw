package csw.services.location.internal

import akka.Done
import akka.actor.{ActorSystem, Terminated}
import akka.cluster.Cluster

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.control.NonFatal

/**
  * Manages termination of `ActorSystem` and gracefully disconnecting from cluster
  */
object Terminator {
  //do not use the dying actorSystem's dispatcher for scheduling actions after its death.
  import ExecutionContext.Implicits.global

  /**
    * If no seed node is provided then the cluster is initialized for `ActorSystem`  by self joining
    */
  def initialize(actorSystem: ActorSystem): Unit = {
    val cluster = Cluster(actorSystem)
    val emptySeeds = actorSystem.settings.config.getStringList("akka.cluster.seed-nodes").isEmpty
    if (emptySeeds) {
      cluster.join(cluster.selfAddress)
    }
    val p = Promise[Done]
    cluster.registerOnMemberUp(p.success(Done))
    try {
      Await.result(p.future, 10.seconds)
    } catch {
      case NonFatal(ex) ⇒
        Await.result(Terminator.terminate(actorSystem), 10.seconds)
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
