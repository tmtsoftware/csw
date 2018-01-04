package csw.messages.ccs.commands.matchers

import akka.util.Timeout
import csw.messages.params.generics.Parameter
import csw.messages.params.states.{CurrentState, DemandState}

/**
 * A StateMatcher which matches the CurrentState against the DemandState
 * @param demand a DemandState that will provide the items for determining completion with the CurrentState
 */
case class DemandMatcherAll(demand: DemandState, timeout: Timeout) extends StateMatcher {
  def prefix: String = demand.prefixStr

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
 */
case class DemandMatcher(demand: DemandState, withUnits: Boolean = false, timeout: Timeout) extends StateMatcher {

  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = {
    demand.paramSet.forall { di =>
      val foundItem: Option[Parameter[_]] = current.find(di)
      foundItem.fold(false)(if (withUnits) _ == di else _.values.sameElements(di.values))
    }
  }
}

/**
 * PresenceMatcher only checks for the existence of a CurrentState with a given prefix.
 * @param prefix the prefix to match against the CurrentState
 */
case class PresenceMatcher(prefix: String, timeout: Timeout) extends StateMatcher {
  def check(current: CurrentState): Boolean = true
}
