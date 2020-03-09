package example.tutorial.moderate.samplehcd

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorRefOps
import csw.framework.models.CswContext
import csw.params.commands.CommandResponse.{Cancelled, Completed}
import csw.params.commands.Result
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

object SleepWorker {
  import example.tutorial.moderate.shared.SampleInfo._

  // SleepWorker splits the sleep time into slices of this many milliseconds so it has a chance to check the cancel flag
  private val slice: Long = 500

  sealed trait SleepWorkerMessages
  case class Sleep(runId: Id, sleepTime: Long)               extends SleepWorkerMessages
  case object Cancel                                         extends SleepWorkerMessages
  case class Tick(runId: Id, current: Long, sleepTime: Long) extends SleepWorkerMessages

  def apply(cswContext: CswContext): Behavior[SleepWorkerMessages] = {
    var cancelFlag = false

    Behaviors.receive { (ctx, message) =>
      message match {
        case Sleep(runId, sleepTime) =>
          val firstSlice    = if (sleepTime < slice) sleepTime else slice
          val when: UTCTime = UTCTime.after(FiniteDuration(firstSlice, MILLISECONDS))
          cswContext.timeServiceScheduler.scheduleOnce(when, ctx.self.toClassic, Tick(runId, firstSlice, sleepTime))
          Behaviors.same
        case Tick(runId, current, sleepTime) =>
          if (cancelFlag || current >= sleepTime) {
            if (cancelFlag) {
              cswContext.commandResponseManager.updateCommand(Cancelled(runId))
            }
            else {
              cswContext.commandResponseManager.updateCommand(Completed(runId, Result().madd(resultKey.set(current))))
            }
            Behaviors.stopped
          }
          else {
            // Schedule another period
            // If slice is more than needed, then use what is left
            val nextSlice = if (current + slice > sleepTime) {
              sleepTime - current
            }
            else {
              slice
            }
            cswContext.timeServiceScheduler.scheduleOnce(
              UTCTime.after(FiniteDuration(slice, MILLISECONDS)),
              ctx.self.toClassic,
              Tick(runId, current + nextSlice, sleepTime)
            )
            Behaviors.same
          }
        case Cancel =>
          // Set the cancel flag to true, next Tick will cancel
          cancelFlag = true
          Behaviors.same
      }
    }
  }
}
