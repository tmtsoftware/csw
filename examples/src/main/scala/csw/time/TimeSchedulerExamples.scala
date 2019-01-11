package csw.time

import akka.actor.ActorSystem
import csw.time.client.TimeServiceSchedulerFactory
import csw.time.client.api.TimeServiceScheduler

object TimeSchedulerExamples {
  implicit val actorSystem: ActorSystem = ActorSystem()

  //#create-scheduler
  // create time service scheduler using the factory method
  private val scheduler: TimeServiceScheduler = TimeServiceSchedulerFactory.make()
  //#create-scheduler

}
