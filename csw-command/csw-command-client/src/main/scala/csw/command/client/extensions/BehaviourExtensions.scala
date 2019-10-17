package csw.command.client.extensions

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

object BehaviourExtensions {

  class WithRunnableInterceptor[T: ClassTag] extends BehaviorInterceptor[Any, T] {
    override def aroundReceive(ctx: TypedActorContext[Any], msg: Any, target: BehaviorInterceptor.ReceiveTarget[T]): Behavior[T] =
      msg match {
        case x: T        => target(ctx, x)
        case x: Runnable => x.run(); Behaviors.same
        case _           => Behaviors.unhandled
      }

    override def aroundSignal(
        ctx: TypedActorContext[Any],
        signal: Signal,
        target: BehaviorInterceptor.SignalTarget[T]
    ): Behavior[T] =
      target(ctx, signal)
  }

  def withSafeEc[T: ClassTag](factory: ExecutionContext => Behavior[T]): Behavior[T] = {
    withRunnableRef[T] { actorRef =>
      val ec = new ExecutionContext {
        override def execute(runnable: Runnable): Unit = actorRef ! runnable

        override def reportFailure(cause: Throwable): Unit = cause.printStackTrace()
      }
      factory(ec)
    }
  }

  def withRunnableRef[T: ClassTag](factory: ActorRef[Runnable] => Behavior[T]): Behavior[T] = {
    Behaviors
      .setup[Any] { ctx =>
        Behaviors.intercept[Any, T](new WithRunnableInterceptor[T])(factory(ctx.self))
      }
      .narrow
  }
}
