package csw.common.components.command

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import csw.framework.models.CswContext
import csw.framework.scaladsl.TopLevelComponent._
import csw.params.commands.CommandResponse.{Accepted, Completed, Invalid, Started}
import csw.common.components.command.CommandComponentState._
import csw.framework.exceptions.FailureRestart
import csw.params.commands.CommandIssue
import csw.time.core.models.UTCTime

object TestComponent {

  case class MyFailure(msg: String) extends FailureRestart(s"What the Fuck!! + $msg")

  case class MyInitState(val1: String, val2: String)

  def apply(cswCtx: CswContext): Behavior[InitializeMessage] = {
    Behaviors.setup { context: ActorContext[InitializeMessage] =>
      println("TLA YES")
      Behaviors.receiveMessage[InitializeMessage] {
        case Initialize(ref) =>
          context.log.debug("Initializing")
          println(s"Initializing for: $ref")
          //throw MyFailure("WTF")
          //val runningActor: ActorRef[RunningMessages] = context.spawn(running(cswCtx), "runner")
          val returnvalue = running(cswCtx, MyInitState("kim", "gillies"))
          ref ! InitializeSuccess(returnvalue)
          println(s"Send Initialize Success to: $ref")
          Behaviors.same
        case _ =>
          println("Got something else")
          Behaviors.same
      }.receiveSignal {
        case (_, signal) =>
          println(s"++++++++++++++++++++++Init actor got signal: $signal")
          Behaviors.same
      }
    }
  }

  private def running(cswCtx: CswContext, myState: MyInitState): Behavior[RunningMessage] = {
    println("Creating Running")
    Behaviors.receiveMessage[RunningMessage] {
      case Validate2(runId, cmd, svr) =>
        println(s"Validate name: $runId, $cmd")
        cmd.commandName match {
          case `invalidCmd` =>
            svr ! Invalid(runId, CommandIssue.OtherIssue("Invalid"))
          case `immediateCmd` =>
            svr ! Accepted(runId)
          case `longRunningCmd` =>
            svr ! Accepted(runId)
          case _ =>
            svr ! Accepted(runId)
        }
        Behaviors.same
      case Submit2(runId, cmd, svr) =>
        println(s"Submit 2 Command name: $runId, $cmd")
        cmd.commandName match {
          case `immediateCmd`=>
            svr ! Completed (runId)
          case `longRunningCmd` =>
            println("Waiting for 3")
            cswCtx.timeServiceScheduler.scheduleOnce(UTCTime(UTCTime.now().value.plusSeconds(3))) {
              println(s"Sending completed for $runId")
              svr ! Completed(runId)
            }
            println(s"Returning started for: $runId")
            svr ! Started(runId)
          case other =>
            println(s"Test got submit: $other")
            svr ! Completed(runId)
        }
        Behaviors.same
      case Oneway2(runId, cmd) =>
        println(s"Oneway name: $runId, $cmd")
        println("FUCKING A!")
        Behaviors.same
      case Shutdown2(svr) =>
        println("TLA got Shutdown--responding success")
        println(s"MyState: $myState")
        svr ! ShutdownSuccessful
        Behaviors.same
      case GoOnline2(svr) =>
        println("TLA got GoOnline")
        svr ! OnlineSuccess
        Behaviors.same
      case GoOffline2(svr) =>
        println("TLA Got GoOffline")
        svr ! OfflineSuccess
        Behaviors.same
    }
  }.receiveSignal {
       case (context: ActorContext[RunningMessage], PostStop) =>
         println(s"-------------------------------------------------PostStop signal for TLA received")
         Behaviors.same
     }


}
