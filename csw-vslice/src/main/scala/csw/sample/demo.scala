package csw.sample

import akka.NotUsed
import akka.typed.scaladsl.Actor.MutableBehavior
import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, ActorSystem, Behavior}
import csw.sample.Messages.Command.IdleCommand.Initialize
import csw.sample.Messages.Command.InitializedCommand.{Add, Reset}
import csw.sample.Messages.Command.{GetMode, IdleCommand, InitializedCommand, Stop}
import csw.sample.Messages.Mode.{Idle, Initialized}
import csw.sample.Messages.{Command, Mode}

object Messages {
  sealed trait Command
  object Command {
    case class GetMode(replyTo: ActorRef[Mode]) extends Command

    sealed trait IdleCommand extends Command
    object IdleCommand {
      case class Initialize(x: Int, replyTo: ActorRef[Initialized]) extends IdleCommand
    }

    sealed trait InitializedCommand extends Command
    object InitializedCommand {
      case class Add(x: Int)                    extends InitializedCommand
      case class Reset(replyTo: ActorRef[Idle]) extends InitializedCommand
    }

    case object Stop extends IdleCommand with InitializedCommand
  }

  sealed trait Mode
  object Mode {
    case class Idle(ref: ActorRef[IdleCommand])               extends Mode
    case class Initialized(ref: ActorRef[InitializedCommand]) extends Mode
  }
}

object Aggregator {
  def behaviour: Behavior[GetMode] = Actor.mutable[Command](ctx ⇒ new Aggregator(ctx)).narrow
}

class Aggregator(ctx: ActorContext[Command]) extends MutableBehavior[Command] {

  var mode: Mode = Idle(ctx.self)
  var sum: Int   = 0

  override def onMessage(msg: Command): Behavior[Command] = {
    (mode, msg) match {
      case (_, GetMode(replyTo))                   ⇒ replyTo ! mode
      case (m: Idle, x: IdleCommand)               ⇒ onIdleCommand(x)
      case (m: Initialized, x: InitializedCommand) ⇒ onInitializedCommand(x)
      case _                                       ⇒ println(s"msg=$msg can not be handled in mode=$mode")
    }
    this
  }

  def onIdleCommand(idleCommand: IdleCommand): Unit = idleCommand match {
    case Initialize(x, replyTo) =>
      println(s"initialized with value=$x")
      sum = x
      mode = Initialized(ctx.self)
      replyTo ! Initialized(ctx.self)
    case Stop => onStop()
  }

  def onInitializedCommand(initializedCommand: InitializedCommand): Unit = initializedCommand match {
    case Add(x) ⇒
      println(s"adding $x to sum=$sum")
      sum += x
    case Reset(replyTo) ⇒
      println(s"resetting, sum=$sum")
      mode = Idle(ctx.self)
      replyTo ! Idle(ctx.self)
    case Stop ⇒ onStop()
  }

  def onStop(): Unit = {
    println("stopping the aggregator")
    ctx.stop(ctx.self)
  }
}

object Client {
  def behavior(count: Int): Behavior[Mode] = Actor.immutable[Mode] {
    case (ctx, msg) ⇒
      msg match {
        case Idle(ref) ⇒
          if (count < 10) {
            ref ! Initialize(count * 100, ctx.self)
            behavior(count + 1)
          } else {
            ref ! Stop
            Actor.stopped
          }
        case Initialized(ref) ⇒
          ref ! Add(10)
          ref ! Add(20)
          ref ! Reset(ctx.self)
          Actor.same
      }
  }
}

object Demo extends App {
  private val wiring = Actor.deferred[NotUsed] { ctx ⇒
    val aggregator = ctx.spawn(Aggregator.behaviour, "aggregator")
    val client     = ctx.spawn(Client.behavior(0), "client")
    aggregator ! GetMode(client)
    Actor.empty
  }

  ActorSystem("demo", wiring)
}
