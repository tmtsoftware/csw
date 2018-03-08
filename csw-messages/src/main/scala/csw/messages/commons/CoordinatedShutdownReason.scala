package csw.messages.commons

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason

//TODO: what, why, how
trait CoordinatedShutdownReason extends CoordinatedShutdown.Reason

object CoordinatedShutdownReasons {

  /**
   * The shutdown was initiated by exception
   */
  case class FailureReason(reason: Throwable) extends CoordinatedShutdownReason

  /**
   * Scala API: The shutdown was initiated after application finished its job (This is useful for short running applications)
   */
  case object ApplicationFinishedReason extends CoordinatedShutdownReason

  /**
   * Java API: The shutdown was initiated after application finished its job (This is useful for short running applications)
   */
  def applicationFinishedReason: Reason = ApplicationFinishedReason

  /**
   * Scala API: The shutdown was initiated by process termination, e.g. Redis process termination
   */
  case object ProcessTerminatedReason extends CoordinatedShutdownReason

  /**
   * Java API: The shutdown was initiated by process termination, e.g. Redis process termination
   */
  def processTerminatedReason: Reason = ProcessTerminatedReason

  /**
   * Scala API: The shutdown was initiated by actor termination, e.g. In Actors PostStop hook
   */
  case object ActorTerminatedReason extends CoordinatedShutdownReason

  /**
   * Java API: The shutdown was initiated by actor termination, e.g. In Actors PostStop hook
   */
  def actorTerminatedReason: Reason = ActorTerminatedReason

  /**
   * Scala API: The shutdown was initiated by Actor on receiving external Shutdown message
   */
  case object ShutdownMessageReceivedReason extends CoordinatedShutdownReason

  /**
   * Java API: The shutdown was initiated by Actor on receiving external Shutdown message
   */
  def shutdownMessageReceivedReason: Reason = ShutdownMessageReceivedReason

  /**
   * Scala API: The shutdown was initiated by Container when it failed to spawns supervisors
   */
  case object FailedToCreateSupervisorsReason extends CoordinatedShutdownReason

  /**
   * Java API: The shutdown was initiated by Container when it failed to spawns supervisors
   */
  def failedToCreateSupervisorsReason: Reason = FailedToCreateSupervisorsReason

  /**
   * Scala API: The shutdown was initiated by Container when all the actors running within a container gets terminated
   */
  case object AllActorsWithinContainerTerminatedReason extends CoordinatedShutdownReason

  /**
   * Java API: The shutdown was initiated by Container when all the actors running within a container gets terminated
   */
  def allActorsWithinContainerTerminatedReason: Reason = AllActorsWithinContainerTerminatedReason

  /**
   * Scala API: Should only be used in Tests
   */
  case object TestFinishedReason extends CoordinatedShutdownReason

  /**
   * Java API: Should only be used in Tests
   */
  def testFinishedReason: Reason = TestFinishedReason
}
