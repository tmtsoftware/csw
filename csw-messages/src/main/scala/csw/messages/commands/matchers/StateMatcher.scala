package csw.messages.commands.matchers

import akka.util.Timeout
import csw.messages.params.states.CurrentState

/**
 * The base trait to build Matchers to match given state against a predicate
 */
trait StateMatcher {

  /**
   * The prefix of the destination component for which the current state is being matched
   *
   * @return the prefix of destination component
   */
  def prefix: String

  /**
   * The name of the state to match for
   *
   * @return the name of the state
   */
  def stateName: String

  /**
   * A predicate to match the current state
   *
   * @param current current state to be matched as represented by [[csw.messages.params.states.CurrentState]]
   * @return true if match is successful, false otherwise
   */
  def check(current: CurrentState): Boolean

  /**
   * The maximum duration for which the matching is executed if not completed either successfully or unsuccessfully
   */
  def timeout: Timeout
}
