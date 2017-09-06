package csw.common.framework.internal.component

import akka.typed.scaladsl.{Actor, ActorContext}
import akka.typed.{ActorRef, Behavior, PostStop, Signal}
import csw.common.ccs.CommandStatus
import csw.common.framework.exceptions.InitializeTimeOut
import csw.common.framework.internal.extensions.RichFutureExtension.RichFuture
import csw.common.framework.models.CommandMessage.{Oneway, Submit}
import csw.common.framework.models.FromComponentLifecycleMessage.{Initialized, Running}
import csw.common.framework.models.IdleMessage.{InitializationFailed, Initialize}
import csw.common.framework.models.InitialMessage.Run
import csw.common.framework.models.RunningMessage.{DomainMessage, Lifecycle}
import csw.common.framework.models.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.common.framework.models.{RunningMessage, _}
import csw.common.framework.scaladsl.ComponentHandlers

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{DurationDouble, FiniteDuration}
import scala.reflect.ClassTag

object ComponentBehavior {
  val shutdownTimeout: FiniteDuration = 10.seconds
}

class ComponentBehavior[Msg <: DomainMessage: ClassTag](
    ctx: ActorContext[ComponentMessage],
    supervisor: ActorRef[FromComponentLifecycleMessage],
    lifecycleHandlers: ComponentHandlers[Msg]
) extends Actor.MutableBehavior[ComponentMessage] {

  implicit val ec: ExecutionContext = ctx.executionContext

  var mode: ComponentMode = ComponentMode.Idle

  ctx.self ! Initialize

  def onMessage(msg: ComponentMessage): Behavior[ComponentMessage] = {
    (mode, msg) match {
      case (ComponentMode.Idle, x: IdleMessage)           ⇒ onIdle(x)
      case (ComponentMode.Initialized, x: InitialMessage) ⇒ onInitial(x)
      case (ComponentMode.Running, x: RunningMessage)     ⇒ onRun(x)
      case _                                              ⇒ println(s"current context=$mode does not handle message=$msg")
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[ComponentMessage]] = {
    case PostStop ⇒
      lifecycleHandlers
        .onShutdown()
        .within(
          ComponentBehavior.shutdownTimeout,
          ctx.system.scheduler,
          Future.failed(new InitializeTimeOut)
        )
      this
  }

  private def onIdle(x: IdleMessage): Unit = x match {
    case Initialize ⇒
      async {
        await(lifecycleHandlers.initialize())
        mode = ComponentMode.Initialized
        supervisor ! Initialized(ctx.self)
      }.failed.foreach(throwable ⇒ ctx.self ! InitializationFailed(throwable))
    case InitializationFailed(throwable) ⇒ throw throwable
  }

  private def onInitial(x: InitialMessage): Unit = x match {
    case Run ⇒
      async {
        await(lifecycleHandlers.onRun())
        mode = ComponentMode.Running
        lifecycleHandlers.isOnline = true
        supervisor ! Running(ctx.self)
      }
  }

  private def onRun(runningMessage: RunningMessage): Unit = runningMessage match {
    case Lifecycle(message) ⇒ onLifecycle(message)
    case x: Msg             ⇒ lifecycleHandlers.onDomainMsg(x)
    case x: CommandMessage  ⇒ onRunningCompCommandMessage(x)
    case _                  ⇒ println("wrong msg")
  }

  private def onLifecycle(message: ToComponentLifecycleMessage): Unit = message match {
    case GoOnline ⇒
      if (!lifecycleHandlers.isOnline) {
        lifecycleHandlers.isOnline = true
        lifecycleHandlers.onGoOnline()
      }
    case GoOffline ⇒
      if (lifecycleHandlers.isOnline) {
        lifecycleHandlers.isOnline = false
        lifecycleHandlers.onGoOffline()
      }
  }

  def onRunningCompCommandMessage(message: CommandMessage): Unit = {
    val newMessage: CommandMessage = message match {
      case x: Oneway ⇒ x.copy(replyTo = ctx.spawnAnonymous(Actor.ignore))
      case x: Submit ⇒ x
    }
    val validation              = lifecycleHandlers.onControlCommand(newMessage)
    val validationCommandResult = CommandStatus.validationAsCommandStatus(validation)
    message.replyTo ! validationCommandResult
  }

}
