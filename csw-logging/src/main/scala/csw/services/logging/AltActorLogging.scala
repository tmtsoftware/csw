package csw.services.logging

import akka.actor.Actor

/**
 * This trait should be included in Akka Actors to enable logging in cases
 * where the Akka logger is also being used. It uses the name altLog instead
 * of log to avoid this name conflict.
 * Click the visibility All button to see protected
 * members that are defined here.
 * You might also want to un-click the Actor button.
 */
trait AltActorLogging extends Actor {
  private[this] val actorName = self.path.toString

  /**
   * The logging system.
   */
//  def loggingSystem: LoggingSystem = LoggingState.loggingSys

  /**
   * The logger.
   */
  protected lazy val altLog = new Logger(actorName = Some(actorName))

}
