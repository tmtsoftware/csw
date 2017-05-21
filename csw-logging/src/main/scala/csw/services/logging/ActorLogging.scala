package csw.services.logging

import akka.actor.{Actor, ActorPath}
import akka.serialization.Serialization

/**
 * This trait should be included in Akka Actors to enable logging.
 * Click the visibility All button to see protected
 * members that are defined here.
 * You might also want to un-click the Actor button.
 */
trait ActorLogging extends Actor {
  private[this] val actorName = ActorPath.fromString(Serialization.serializedActorPath(self)).toString

  /**
   * The logging system.
   */
//  def loggingSystem: LoggingSystem = LoggingState.loggingSys

  /**
   * The logger.
   */
  protected lazy val log = new Logger(actorName = Some(actorName))

}
