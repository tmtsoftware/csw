package csw.common.components.command

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.Behavior
import csw.framework.models.CswContext
import csw.framework.scaladsl.TopLevelComponent._
import csw.params.commands.CommandResponse.{Accepted, Completed}
import scala.concurrent.duration.DurationInt

object TestCompInitFailureRestart {

  def apply(cswCtx: CswContext): Behavior[InitializeMessage] = {
    Behaviors.setup { context: ActorContext[InitializeMessage] =>
      println("TestCompFailureRestart YES")
      Behaviors
        .receiveMessage[InitializeMessage] {
          case Initialize(ref) =>
            context.log.debug("Initializing")
            println(s"Initializing: $ref")
            //val runningActor: ActorRef[RunningMessages] = context.spawn(running(cswCtx), "runner")
            ref ! InitializeFailureRestart
            println(s"Send Initialize Failure Restart to: $ref")
            Behaviors.same
          case _ =>
            println("Got something else")
            Behaviors.same
        }
        .receiveSignal {
          case (_, signal) =>
            println(s"Got signal: $signal")
            Behaviors.same
        }
    }
  }

  private def running(cswCtx: CswContext): Behavior[RunningMessage] =
    Behaviors.receiveMessage {
      case Validate2(runId, cmd, svr) =>
        println(s"Validate name: $runId, $cmd")
        svr ! Accepted(runId)
        Behaviors.same
      case Submit2(runId, cmd, svr) =>
        println(s"Command: $runId, $cmd")
        svr ! Completed(runId)
        Behaviors.same
      case Oneway2(runId, cmd) =>
        println(s"Oneway name: $runId, $cmd")
        Behaviors.same
    }

}
