package csw.trombone.assembly

import csw.messages.ccs.commands.DemandMatcher
import csw.messages.params.states.DemandState
import csw.trombone.hcd.TromboneHcdState

import scala.concurrent.duration.DurationLong

object AssemblyMatchers {
  def idleMatcher: DemandMatcher =
    DemandMatcher(
      DemandState(TromboneHcdState.axisStateCK).add(TromboneHcdState.stateKey -> TromboneHcdState.AXIS_IDLE),
      timeout = 5.seconds
    )

  def posMatcher(position: Int): DemandMatcher =
    DemandMatcher(
      DemandState(TromboneHcdState.axisStateCK)
        .madd(TromboneHcdState.stateKey -> TromboneHcdState.AXIS_IDLE, TromboneHcdState.positionKey -> position),
      timeout = 5.seconds
    )
}
