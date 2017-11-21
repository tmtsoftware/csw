package csw.ccs.internal.matchers

import akka.util.Timeout
import csw.messages.params.generics.Parameter
import csw.messages.params.states.{CurrentState, DemandState}

trait StateMatcher {
  def prefix: String
  def check(current: CurrentState): Boolean
  def timeout: Timeout
}

case class DemandMatcherAll(demand: DemandState, timeout: Timeout) extends StateMatcher {
  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = demand.paramSet == current.paramSet
}

case class DemandMatcher(demand: DemandState, withUnits: Boolean = false, timeout: Timeout) extends StateMatcher {

  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = {
    demand.paramSet.forall { di =>
      val foundItem: Option[Parameter[_]] = current.find(di)
      foundItem.fold(false)(if (withUnits) _ == di else _.values.sameElements(di.values))
    }
  }
}

case class PresenceMatcher(prefix: String, timeout: Timeout) extends StateMatcher {
  def check(current: CurrentState) = true
}
