package example.time

import java.time.Duration

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter.{TypedActorRefOps, TypedActorSystemOps}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.{ActorRef, ActorSystem}
import csw.time.core.models.UTCTime
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler

class SchedulerExamples(ctx: ActorContext[UTCTime]) {

  implicit val actorSystem: ActorSystem = ctx.system.toUntyped
  //#create-scheduler
  // create time service scheduler using the factory method
  private val scheduler: TimeServiceScheduler = TimeServiceSchedulerFactory.make()(actorSystem)
  //#create-scheduler

  private val utcTime = UTCTime.now()

  // #schedule-once
  scheduler.scheduleOnce(utcTime) {
    // do something
  }
  // #schedule-once

  // #schedule-once-with-actorRef
  object SchedulingHandler {
    def behavior: Behavior[UTCTime] = Behaviors.setup { ctx ⇒
      //setup required for the actor

      Behaviors.receiveMessage {
        case _ ⇒ // handle the message to execute the task on scheduled time and return new behavior
          Behaviors.same
      }
    }
  }

  private val actorRef: ActorRef = ctx.spawnAnonymous(SchedulingHandler.behavior).toUntyped

  scheduler.scheduleOnce(utcTime, actorRef, UTCTime.now())

  // #schedule-once-with-actorRef

  // #schedule-periodically
  scheduler.schedulePeriodically(Duration.ofMillis(50)) { /* do something*/ }
  // #schedule-periodically

  // #schedule-periodically-with-startTime
  scheduler.schedulePeriodically(utcTime, Duration.ofMillis(50)) { /* do something*/ }
  // #schedule-periodically-with-startTime

}
