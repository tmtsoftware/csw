package csw.framework.internal.pubsub

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import csw.command.client.models.framework.PubSub
import csw.command.client.models.framework.PubSub._
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.commands.Nameable
import csw.params.core.states.StateName

/**
 * The actor which can be used by a component to publish its data of a given type, to all the components who subscribe
 */
private[framework] object PubSubBehavior {

  /**
   * @param loggerFactory the LoggerFactory used for logging with component name
   * @tparam T the type of the data which will be published or subscribed to using this actor
   * @return Behavior[ PubSub[T] ] of actor which can be used by a component to publish its data of a given type, to all the components who subscribe
   */
  def make[T: Nameable](loggerFactory: LoggerFactory): Behavior[PubSub[T]] = Behaviors.setup { ctx ⇒
    val log: Logger = loggerFactory.getLogger(ctx)

    val nameableData: Nameable[T] = implicitly[Nameable[T]]

    def subscribe(subscribers: Map[ActorRef[T], Set[StateName]], ref: ActorRef[T], names: Set[StateName]): Behavior[PubSub[T]] = {
      ctx.watchWith(ref, Unsubscribe(ref))
      receive(subscribers + (ref → names))
    }

    def notify(data: T, subscribers: Map[ActorRef[T], Set[StateName]]): Unit = {
      log.debug(s"Notifying subscribers :[${subscribers.mkString(",")}] with data :[$data]")
      subscribers.foreach {
        case (actorRef, names) ⇒ if (names.isEmpty || names.contains(nameableData.name(data))) actorRef ! data
      }
    }

    def receive(subscribers: Map[ActorRef[T], Set[StateName]]): Behavior[PubSub[T]] =
      Behaviors
        .receiveMessage[PubSub[T]] {
          case SubscribeOnly(ref, names) => subscribe(subscribers, ref, names)
          case Subscribe(ref)            => subscribe(subscribers, ref, Set.empty)
          case Unsubscribe(ref)          => ctx.unwatch(ref); receive(subscribers - ref)
          case Publish(data)             => notify(data, subscribers); Behaviors.same
        }

    receive(Map.empty)
  }
}
