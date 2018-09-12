package csw.framework.commons

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason

object CoordinatedShutdownReasons {

  /**
   * The shutdown was initiated by exception
   *
   * @param reason represents the cause in terms of exception
   */
  case class FailureReason(reason: Throwable) extends CoordinatedShutdown.Reason

  /**
   * Scala API: The shutdown was initiated after application finished its job (This is useful for short running applications)
   */
  case object ApplicationFinishedReason extends CoordinatedShutdown.Reason

  /**
   * Java API: The shutdown was initiated after application finished its job (This is useful for short running applications)
   */
  def applicationFinishedReason: Reason = ApplicationFinishedReason

  /**
   * Scala API: The shutdown was initiated by actor termination, e.g. In Actors PostStop hook
   */
  case object ActorTerminatedReason extends CoordinatedShutdown.Reason

  /**
   * Java API: The shutdown was initiated by actor termination, e.g. In Actors PostStop hook
   */
  def actorTerminatedReason: Reason = ActorTerminatedReason

  /**
   * Scala API: The shutdown was initiated by Actor on receiving external Shutdown message
   */
  case object ShutdownMessageReceivedReason extends CoordinatedShutdown.Reason

  /**
   * Java API: The shutdown was initiated by Actor on receiving external Shutdown message
   */
  def shutdownMessageReceivedReason: Reason = ShutdownMessageReceivedReason

  /**
   * Scala API: The shutdown was initiated by Container when it failed to spawns supervisors
   */
  case object FailedToCreateSupervisorsReason extends CoordinatedShutdown.Reason

  /**
   * Java API: The shutdown was initiated by Container when it failed to spawns supervisors
   */
  def failedToCreateSupervisorsReason: Reason = FailedToCreateSupervisorsReason

  /**
   * Scala API: The shutdown was initiated by Container when all the actors running within a container gets terminated
   */
  case object AllActorsWithinContainerTerminatedReason extends CoordinatedShutdown.Reason

  /**
   * Java API: The shutdown was initiated by Container when all the actors running within a container gets terminated
   */
  def allActorsWithinContainerTerminatedReason: Reason = AllActorsWithinContainerTerminatedReason

}
