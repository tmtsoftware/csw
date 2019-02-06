package example.time

import java.time.Duration

import akka.actor.{ActorRef, ActorSystem}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter.{TypedActorRefOps, TypedActorSystemOps}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import csw.time.api.models.UTCTime
import csw.time.client.TimeServiceSchedulerFactory
import csw.time.client.api.TimeServiceScheduler

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
  class SchedulingHandler(ctx: ActorContext[UTCTime]) extends AbstractBehavior[UTCTime] {
    override def onMessage(msg: UTCTime): Behavior[UTCTime] = {
      // handle the message to execute the task on scheduled time
      Behaviors.same
    }
  }

  private val behavior: Behavior[UTCTime] = Behaviors.setup(ctx ⇒ new SchedulingHandler(ctx))
  private val actorRef: ActorRef          = ctx.spawnAnonymous(behavior).toUntyped

  scheduler.scheduleOnce(utcTime, actorRef, UTCTime.now())

  // #schedule-once-with-actorRef

  // #schedule-periodically
  scheduler.schedulePeriodically(Duration.ofMillis(50)) { /* do something*/ }
  // #schedule-periodically

  // #schedule-periodically-with-startTime
  scheduler.schedulePeriodically(utcTime, Duration.ofMillis(50)) { /* do something*/ }
  // #schedule-periodically-with-startTime

}
