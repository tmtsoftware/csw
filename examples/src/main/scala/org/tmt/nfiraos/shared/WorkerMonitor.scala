package org.tmt.nfiraos.shared

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import csw.framework.models.CswContext
import csw.params.core.models.Id

object WorkerMonitor {
  case class Response[T](response: T)

  sealed trait WorkerMonitorMessages
  case class AddWorker[T](runId: Id, worker: T) extends WorkerMonitorMessages
  case class GetWorker[T](runId: Id, replyTo: ActorRef[Response[T]]) extends WorkerMonitorMessages
  case class RemoveWorker(runId: Id) extends WorkerMonitorMessages
  case object Info extends WorkerMonitorMessages


  def apply[T](cswCtx: CswContext): Behavior[WorkerMonitorMessages] = {
    Behaviors.setup(_ => monitor[T](cswCtx, Map.empty[Id,T]))
  }

  private def monitor[T](cswCtx: CswContext, workerMap: Map[Id, T]): Behavior[WorkerMonitorMessages] =
    Behaviors.receive { (_, message) =>
      message match {
        case AddWorker(runId, worker) =>

          val newMap = workerMap + (runId -> worker)
          println("Add WorkerMap: " + newMap)
          monitor(cswCtx, newMap)
/*
        case CancelSleep(runId) =>
          println(s"Monitor cancelling sleep: $runId")
          workerMap.get(runId).map(_ ! SleepWorkerWithMonitor.Cancel)
          ctx.self ! RemoveWorker(runId)
          Behaviors.same
*/
        case GetWorker(runId, replyTo) =>
          println(s"Got to GetWorker: $runId $replyTo")
          val worker:T = workerMap(runId)
          println("Getted Worker is: " + worker)
          replyTo ! Response(worker)
          Behaviors.same

        case RemoveWorker(runId) =>
          println(s"Removing: $runId")
          val newMap = workerMap - runId
          println("Remove WorkerMap: " + newMap)
          monitor(cswCtx, newMap)

        case Info =>
          println("Info WorkerMap: " + workerMap)
          Behaviors.same
      }
    }

}