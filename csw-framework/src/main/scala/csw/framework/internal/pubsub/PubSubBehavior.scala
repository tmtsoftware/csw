package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import akka.actor.typed.{ActorRef, Behavior, Signal, Terminated}
import csw.command.client.models.framework.PubSub
import csw.command.client.models.framework.PubSub._
import csw.logging.core.scaladsl.{Logger, LoggerFactory}
import csw.params.commands.Nameable
import csw.params.core.states.StateName

/**
 * The actor which can be used by a component to publish its data of a given type, to all the components who subscribe
 *
 * @param ctx the Actor Context under which the actor instance of this behavior is created
 * @param loggerFactory the LoggerFactory used for logging with component name
 * @tparam T the type of the data which will be published or subscribed to using this actor
 */
private[framework] class PubSubBehavior[T: Nameable](ctx: ActorContext[PubSub[T]], loggerFactory: LoggerFactory)
    extends AbstractBehavior[PubSub[T]] {
  private val log: Logger = loggerFactory.getLogger(ctx)

  val nameableData: Nameable[T] = implicitly[Nameable[T]]
  // list of subscribers who subscribe to the component using this pub-sub actor for the data of type [[T]]
  var subscribers: Map[ActorRef[T], Option[Set[StateName]]] = Map.empty

  override def onMessage(msg: PubSub[T]): Behavior[PubSub[T]] = {
    msg match {
      case SubscribeOnly(ref, names) => subscribe(ref, Some(names))
      case Subscribe(ref)            => subscribe(ref, None)
      case Unsubscribe(ref)          => unsubscribe(ref)
      case Publish(data)             => notifySubscribers(data)
    }
    this
  }

  override def onSignal: PartialFunction[Signal, Behavior[PubSub[T]]] = {
    case Terminated(ref) ⇒ unsubscribe(ref.unsafeUpcast); this
  }

  private def subscribe(actorRef: ActorRef[T], mayBeNames: Option[Set[StateName]]): Unit =
    if (!subscribers.contains(actorRef)) {
      subscribers += ((actorRef, mayBeNames))
      ctx.watch(actorRef)
    }

  private def unsubscribe(actorRef: ActorRef[T]): Unit = subscribers -= actorRef

  protected def notifySubscribers(data: T): Unit = {
    log.debug(s"Notifying subscribers :[${subscribers.mkString(",")}] with data :[$data]")
    subscribers.foreach {
      case (actorRef, None)        ⇒ actorRef ! data
      case (actorRef, Some(names)) ⇒ if (names.contains(nameableData.name(data))) actorRef ! data
    }
  }
}
