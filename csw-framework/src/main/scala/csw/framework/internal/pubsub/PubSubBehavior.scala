package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.{ActorContext, MutableBehavior}
import akka.actor.typed.{ActorRef, Behavior, Signal, Terminated}
import csw.messages.commands.SubscriptionKey
import csw.messages.framework.PubSub
import csw.messages.framework.PubSub.{Publish, Subscribe, SubscribeOnly, Unsubscribe}
import csw.services.logging.scaladsl.{Logger, LoggerFactory}

/**
 * The actor which can be used by a component to publish its data of a given type, to all the components who subscribe
 *
 * @param ctx the Actor Context under which the actor instance of this behavior is created
 * @param loggerFactory the LoggerFactory used for logging with component name
 * @tparam T the type of the data which will be published or subscribed to using this actor
 */
private[framework] class PubSubBehavior[T](ctx: ActorContext[PubSub[T]], loggerFactory: LoggerFactory)
    extends MutableBehavior[PubSub[T]] {
  private val log: Logger = loggerFactory.getLogger(ctx)

  // list of subscribers who subscribe to the component using this pub-sub actor for the data of type [[T]]
  var subscribers: Map[ActorRef[T], SubscriptionKey[T]] = Map.empty

  override def onMessage(msg: PubSub[T]): Behavior[PubSub[T]] = {
    msg match {
      case SubscribeOnly(ref, key) => subscribe(ref, key)
      case Subscribe(ref)          => subscribe(ref, SubscriptionKey.all)
      case Unsubscribe(ref)        => unsubscribe(ref)
      case Publish(data)           => notifySubscribers(data)
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[PubSub[T]]] = {
    case Terminated(ref) ⇒ unsubscribe(ref.upcast); this
  }

  private def subscribe(actorRef: ActorRef[T], key: SubscriptionKey[T]): Unit = {
    if (!subscribers.contains(actorRef)) {
      subscribers += ((actorRef, key))
      ctx.watch(actorRef)
    }
  }

  private def unsubscribe(actorRef: ActorRef[T]): Unit = {
    subscribers -= actorRef
  }

  protected def notifySubscribers(data: T): Unit = {
    log.debug(s"Notifying subscribers :[${subscribers.mkString(",")}] with data :[$data]")
    subscribers.filter(subscriber ⇒ subscriber._2.predicate(data)).foreach(_._1 ! data)
  }
}
