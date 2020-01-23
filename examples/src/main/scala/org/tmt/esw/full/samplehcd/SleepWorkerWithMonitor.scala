package org.tmt.esw.full.samplehcd

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import akka.actor.typed.{ActorRef, Behavior}
import csw.framework.models.CswContext
import csw.params.commands.CommandResponse.{Cancelled, Completed}
import csw.params.commands.Result
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import org.tmt.esw.full.shared.WorkerMonitor
import org.tmt.esw.full.shared.WorkerMonitor.WorkerMonitorMessages

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object SleepWorkerWithMonitor {
  import org.tmt.esw.moderate.shared.SampleInfo._

  // SleepWorker splits the sleep time into slices of this many milliseconds so it has a chance to check the cancel flag
  private val slice: Long = 500

  sealed trait SleepWorkerWithMonitorMessages
  case class Sleep(runId: Id, sleepTime: Long, monitor: ActorRef[WorkerMonitorMessages]) extends SleepWorkerWithMonitorMessages
  case object Cancel                                                                     extends SleepWorkerWithMonitorMessages
  case class Tick(runId: Id, current: Long, sleepTime: Long, monitor: ActorRef[WorkerMonitorMessages])
      extends SleepWorkerWithMonitorMessages

  def apply(cswContext: CswContext): Behavior[SleepWorkerWithMonitorMessages] = {
    var cancelFlag = false

    Behaviors.receive { (ctx, message) =>
      message match {
        case Sleep(runId, sleepTime, monitor) =>
          val firstSlice    = if (sleepTime < slice) sleepTime else slice
          val when: UTCTime = UTCTime.after(FiniteDuration(firstSlice, MILLISECONDS))
          cswContext.timeServiceScheduler.scheduleOnce(when, ctx.self.toClassic, Tick(runId, firstSlice, sleepTime, monitor))
          Behaviors.same
        case Tick(runId, current, sleepTime, monitor) =>
          if (cancelFlag || current >= sleepTime) {
            monitor ! WorkerMonitor.RemoveWorker(runId)
            if (cancelFlag) {
              cswContext.commandResponseManager.updateCommand(Cancelled(runId))
              println(s"Worker cancelled at: $current")
            }
            else {
              println(s"Worker times up at: $current")
              cswContext.commandResponseManager.updateCommand(Completed(runId, Result().madd(resultKey.set(current))))
            }
            Behaviors.stopped
          }
          else {
            // Schedule another period
            //println(s"Current: $current $cancelFlag")

            // If slice is more than needed, then use what is left
            val nextSlice = if (current + slice > sleepTime) {
              sleepTime - current
            }
            else {
              slice
            }
            cswContext.timeServiceScheduler.scheduleOnce(
              UTCTime.after(FiniteDuration(slice, MILLISECONDS)),
              (ctx.self).toClassic,
              Tick(runId, current + nextSlice, sleepTime, monitor)
            )
            Behaviors.same
          }
        case Cancel =>
          // If Cancel received, set flag to true that causes early end at the next tick
          cancelFlag = true
          Behaviors.same
      }
    }
  }
}
