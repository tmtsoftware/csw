package csw.services.location.internal

import akka.Done
import akka.actor.{ActorSystem, Terminated}
import akka.cluster.Cluster

import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Manages termination of `ActorSystem` and gracefully disconnecting from cluster
  */
object Terminator {
  //do not use the dying actorSystem's dispatcher for scheduling actions after its death.
  import ExecutionContext.Implicits.global

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
