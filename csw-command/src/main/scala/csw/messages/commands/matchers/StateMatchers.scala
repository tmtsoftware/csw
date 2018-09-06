package csw.messages.commands.matchers

import akka.util.Timeout
import csw.messages.params.generics.Parameter
import csw.messages.params.states.{CurrentState, DemandState}

/**
 * A StateMatcher which matches the CurrentState against the DemandState
 *
 * @param demand a DemandState that will provide the items for determining completion with the CurrentState
 * @param timeout a timeout for which the matching should be executed. Once the timeout occurs, complete the match with
 *                MatchFailed response and appropriate failure exception.
 */
case class DemandMatcherAll(demand: DemandState, timeout: Timeout) extends StateMatcher {

  /**
   * The prefix of the destination component for which the current state is being matched
   *
   * @return the prefix of destination component
   */
  def prefix: String = demand.prefixStr

  /**
   * The name of the state to match for
   *
   * @return the name of the state
   */
  def stateName: String = demand.stateName.name

  /**
   * A predicate to match the current state
   *
   * @param current current state to be matched as represented by [[csw.messages.params.states.CurrentState]]
   * @return true if match is successful, false otherwise
   */
  def check(current: CurrentState): Boolean = demand.paramSet == current.paramSet

}

/**
 * The DemandMatcher checks the CurrentStatus for equality with the items in the DemandState.
 * This version tests for equality so it may not work the best with floating point values.
 * Note: If the withUnits flag is set, the equality check with also compare units. False is the default
 * so normally units are ignored for this purpose.
 *
 * @param demand a DemandState that will provide the items for determining completion with the CurrentState
 * @param withUnits when True, units are compared. When false, units are not compared. Default is false.
 * @param timeout a timeout for which the matching should be executed. Once the timeout occurs, complete the match with
 *                MatchFailed response and appropriate failure exception.
 */
case class DemandMatcher(demand: DemandState, withUnits: Boolean = false, timeout: Timeout) extends StateMatcher {

  /**
   * The prefix of the destination component for which the current state is being matched
   *
   * @return the prefix of destination component
   */
  def prefix: String = demand.prefixStr

  /**
   * The name of the state to match for
   *
   * @return the name of the state
   */
  def stateName: String = demand.stateName.name

  /**
   * A predicate to match the current state
   *
   * @param current current state to be matched as represented by [[csw.messages.params.states.CurrentState]]
   * @return true if match is successful, false otherwise
   */
  def check(current: CurrentState): Boolean = {
    demand.paramSet.forall { di =>
      val foundItem: Option[Parameter[_]] = current.find(di)
      foundItem.fold(false)(if (withUnits) _ == di else _.values.sameElements(di.values))
    }
  }
}

/**
 * PresenceMatcher only checks for the existence of a CurrentState with a given prefix and name
 *
 * @param prefix the prefix to match against the CurrentState
 * @param stateName the name to match against the stateName of CurrentState
 * @param timeout A timeout for which the matching should be executed. Once the timeout occurs, complete the match with
 *                MatchFailed response and appropriate failure exception.
 */
case class PresenceMatcher(prefix: String, stateName: String, timeout: Timeout) extends StateMatcher {

  /**
   * A predicate to match the current state
   *
   * @param current current state to be matched as represented by [[csw.messages.params.states.CurrentState]]
   * @return true if match is successful, false otherwise
   */
  def check(current: CurrentState): Boolean = prefix == current.prefixStr && stateName == current.stateName.name
}
