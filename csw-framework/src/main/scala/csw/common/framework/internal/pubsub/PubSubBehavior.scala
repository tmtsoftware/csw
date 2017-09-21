package csw.common.framework.internal.pubsub

import akka.typed.scaladsl.ActorContext
import akka.typed.{ActorRef, Behavior, Signal, Terminated}
import csw.common.framework.models.PubSub
import csw.common.framework.models.PubSub.{Publish, Subscribe, Unsubscribe}
import csw.services.logging.scaladsl.ComponentLogger

class PubSubBehavior[T](ctx: ActorContext[PubSub[T]], componentName: String)
    extends ComponentLogger.TypedActor[PubSub[T]](ctx, Some(componentName)) {

  var subscribers: Set[ActorRef[T]] = Set.empty

  override def onMessage(msg: PubSub[T]): Behavior[PubSub[T]] = {
    msg match {
      case Subscribe(ref)   => subscribe(ref)
      case Unsubscribe(ref) => unsubscribe(ref)
      case Publish(data)    => notifySubscribers(data)
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[PubSub[T]]] = {
    case Terminated(ref) ⇒ unsubscribe(ref.upcast); this
  }

  private def subscribe(actorRef: ActorRef[T]): Unit = {
    if (!subscribers.contains(actorRef)) {
      subscribers += actorRef
      ctx.watch(actorRef)
    }
  }

  private def unsubscribe(actorRef: ActorRef[T]): Unit = {
    subscribers -= actorRef
  }

  protected def notifySubscribers(data: T): Unit = {
    log.debug(s"Notifying subscribers :[${subscribers.mkString(",")}] with data :[$data]")
    subscribers.foreach(_ ! data)
  }
}
