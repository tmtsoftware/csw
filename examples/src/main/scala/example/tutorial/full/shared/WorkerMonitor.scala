package example.tutorial.full.shared

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.logging.api.scaladsl.Logger
import csw.params.core.models.Id

/**
 * Worker Monitor is a somewhat general Map actor for associating runId with various things
 * given by the type T and runId as key. Messages exist to add a worker, remove a worker,
 * get a worker associated with a key, and to print out the map for diag purposes.
 */
object WorkerMonitor {
  case class Response[T](response: T)

  sealed trait WorkerMonitorMessages
  case class AddWorker[T](runId: Id, worker: T)                      extends WorkerMonitorMessages
  case class GetWorker[T](runId: Id, replyTo: ActorRef[Response[T]]) extends WorkerMonitorMessages
  case class RemoveWorker(runId: Id)                                 extends WorkerMonitorMessages
  case object Info                                                   extends WorkerMonitorMessages

  def apply[T](cswCtx: CswContext): Behavior[WorkerMonitorMessages] = {
    val logger = cswCtx.loggerFactory.getLogger
    Behaviors.setup(_ => monitor[T](logger, Map.empty[Id, T]))
  }

  private def monitor[T](logger: Logger, workerMap: Map[Id, T]): Behavior[WorkerMonitorMessages] =
    Behaviors.receive { (_, message) =>
      message match {
        case AddWorker(runId, worker) =>
          val newMap = workerMap + (runId -> worker)
          logger.debug(s"Worker monitor added. New map: $newMap")
          monitor(logger, newMap)

        case GetWorker(runId, replyTo) =>
          val worker: T = workerMap(runId)
          replyTo ! Response(worker)
          Behaviors.same

        case RemoveWorker(runId) =>
          logger.debug(s"Worker monitor removing: $runId")
          val newMap = workerMap - runId
          logger.debug("Worker monitor map after remove: $newMap")
          monitor(logger, newMap)

        case Info =>
          println("Info WorkerMap: " + workerMap)
          Behaviors.same
      }
    }

}
