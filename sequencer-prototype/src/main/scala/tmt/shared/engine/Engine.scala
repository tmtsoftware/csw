package tmt.shared.engine

import akka.actor.Scheduler
import akka.typed.scaladsl.AskPattern._
import akka.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import EngineBehaviour._
import tmt.shared.engine.EngineBehaviour._
import tmt.shared.util.FutureExt.RichFuture
import tmt.shared.services.Command

import scala.concurrent.duration.DurationLong

class Engine(engine: ActorRef[EngineAction], system: ActorSystem[_]) {
  private implicit val timeout: Timeout     = Timeout(10.hour)
  private implicit val scheduler: Scheduler = system.scheduler

  def pullNext(): Command                    = (engine ? Pull).await
  def push(command: Command): Unit           = engine ! Push(command)
  def pushAll(commands: List[Command]): Unit = commands.foreach(push)
  def hasNext: Boolean                       = (engine ? HasNext).await
  def pause(): Unit                          = engine ! Pause
  def resume(): Unit                         = engine ! Resume
  def reset(): Unit                          = engine ! Reset
}
