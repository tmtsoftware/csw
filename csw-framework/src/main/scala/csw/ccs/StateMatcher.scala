package csw.ccs

import csw.messages.states.{CurrentState, DemandState}
import csw.messages.generics.Parameter

trait StateMatcher {
  def prefix: String

  def check(current: CurrentState): Boolean
}

case class DemandMatcherAll(demand: DemandState) extends StateMatcher {
  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = demand.paramSet == current.paramSet
}

case class DemandMatcher(demand: DemandState, withUnits: Boolean = false) extends StateMatcher {

  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = {
    demand.paramSet.forall { di =>
      val foundItem: Option[Parameter[_]] = current.find(di)
      foundItem.fold(false)(if (withUnits) _ == di else _.values.sameElements(di.values))
    }
  }
}

case class PresenceMatcher(prefix: String) extends StateMatcher {
  def check(current: CurrentState) = true
}
