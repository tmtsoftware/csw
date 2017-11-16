package csw.trombone.assembly

import csw.ccs.internal.matchers.DemandMatcher
import csw.messages.params.states.DemandState
import csw.trombone.hcd.TromboneHcdState

object AssemblyMatchers {
  def idleMatcher: DemandMatcher =
    DemandMatcher(
      DemandState(TromboneHcdState.axisStateCK).add(TromboneHcdState.stateKey -> TromboneHcdState.AXIS_IDLE)
    )

  def posMatcher(position: Int): DemandMatcher =
    DemandMatcher(
      DemandState(TromboneHcdState.axisStateCK)
        .madd(TromboneHcdState.stateKey -> TromboneHcdState.AXIS_IDLE, TromboneHcdState.positionKey -> position)
    )
}
