package csw.logging.commons

import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import csw.logging.internal.LogAdminActor
import csw.logging.messages.LogControlMessages

/**
 * A factory to create the `LogAdminActor`
 */
private[csw] object LogAdminActorFactory {

  /**
   * Create LogAdminActor in an app. It is used while registering any connection in location service.
   *
   * @param actorSystem the ActorSystem used to create the LogAdminActor
   * @return a typed ActorRef understanding LogControlMessages to update or get the log level
   */
  def make(actorSystem: ActorSystem): ActorRef[LogControlMessages] =
    actorSystem.spawnAnonymous(LogAdminActor.behavior())
}
