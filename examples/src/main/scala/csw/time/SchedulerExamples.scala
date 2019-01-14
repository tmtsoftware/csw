package csw.time

import java.time.Duration

import akka.actor.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter.{TypedActorRefOps, TypedActorSystemOps}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import csw.time.api.models.UTCTime
import csw.time.client.TimeServiceSchedulerFactory
import csw.time.client.api.TimeServiceScheduler

import scala.concurrent.duration.DurationInt

class SchedulerExamples(ctx: ActorContext[String]) {
  implicit val actorSystem: ActorSystem = ctx.system.toUntyped

  //#create-scheduler
  // create time service scheduler using the factory method
  private val scheduler: TimeServiceScheduler = TimeServiceSchedulerFactory.make()
  //#create-scheduler

  private val utcTime = UTCTime.now()

  // #schedule-once
  scheduler.scheduleOnce(utcTime) {
    // do something
  }
  // #schedule-once

  // #schedule-once-with-actorRef
  private val behavior: Behavior[String] = Behaviors.setup(ctx ⇒ new SchedulingHandler(ctx))
  private val actorRef                   = ctx.spawnAnonymous(behavior).toUntyped

  scheduler.scheduleOnce(utcTime, actorRef, "some message")

  class SchedulingHandler(ctx: ActorContext[String]) extends AbstractBehavior[String] {
    override def onMessage(msg: String): Behavior[String] = {
      // handle the message to execute the task on scheduled time
      Behaviors.same
    }
  }
  // #schedule-once-with-actorRef

  // #schedule-periodically
  scheduler.schedulePeriodically(Duration.ofMillis(50)) { /* do something*/ }
  // #schedule-periodically

  // #schedule-periodically-with-startTime
  scheduler.schedulePeriodically(utcTime, Duration.ofMillis(50)) { /* do something*/ }
  // #schedule-periodically-with-startTime

}
