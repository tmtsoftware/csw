package csw.services.location.internal

import akka.Done
import akka.actor.{ActorSystem, Terminated}
import akka.cluster.Cluster

import scala.concurrent.{ExecutionContext, Future, Promise}

object Terminator {
  //do not use the dying actorSystem's dispatcher for scheduling actions after its death.
  import ExecutionContext.Implicits.global

  def terminate(actorSystem: ActorSystem): Future[Done] = {
    val cluster = Cluster(actorSystem)
    val p = Promise[Terminated]
    cluster.leave(cluster.selfAddress)
    cluster.registerOnMemberRemoved(actorSystem.terminate().onComplete(p.complete))
    p.future.map { _ =>
      Thread.sleep(300)
      Done
    }
  }
}
