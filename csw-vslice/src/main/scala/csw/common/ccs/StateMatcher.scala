package csw.common.ccs

import csw.param.Parameter
import csw.param.StateVariable.{CurrentState, DemandState}

trait StateMatcher {
  def prefix: String

  def check(current: CurrentState): Boolean
}

case class DemandMatcherAll(demand: DemandState) extends StateMatcher {
  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = demand.paramSet.equals(current.paramSet)
}

case class DemandMatcher(demand: DemandState, withUnits: Boolean = false) extends StateMatcher {

  def prefix: String = demand.prefixStr

  def check(current: CurrentState): Boolean = {
    demand.paramSet.forall { di =>
      val foundItem: Option[Parameter[_]] = current.find(di)
      foundItem.fold(false)(if (withUnits) _.equals(di) else _.values.equals(di.values))
    }
  }
}

case class PresenceMatcher(prefix: String) extends StateMatcher {
  def check(current: CurrentState) = true
}
