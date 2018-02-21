package tmt.shared.dsl

import akka.actor.Scheduler
import akka.typed.scaladsl.AskPattern._
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.messages.ccs.commands.ControlCommand
import tmt.shared.engine.EngineBehavior._
import tmt.shared.util.FutureExt.RichFuture

import scala.concurrent.duration.DurationLong

class EngineDsl(engineRef: ActorRef[EngineAction], system: ActorSystem[_]) {
  private implicit val timeout: Timeout     = Timeout(10.hour)
  private implicit val scheduler: Scheduler = system.scheduler

  def pullNext(): ControlCommand                   = (engineRef ? Pull).await
  def push(command: ControlCommand): Unit          = engineRef ! Push(command)
  def pushAll(commands: Seq[ControlCommand]): Unit = commands.foreach(push)
  def hasNext: Boolean                             = (engineRef ? HasNext).await
  def pause(): Unit                                = engineRef ! Pause
  def resume(): Unit                               = engineRef ! Resume
  def reset(): Unit                                = engineRef ! Reset
}
