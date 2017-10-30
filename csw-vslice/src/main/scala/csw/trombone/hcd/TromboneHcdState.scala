package csw.trombone.hcd

import java.io.File

import csw.messages.ccs.commands.Setup
import csw.messages.params.generics.KeyType.ChoiceKey
import csw.messages.params.generics.{GChoiceKey, Key, KeyType}
import csw.messages.params.models.Units.encoder
import csw.messages.params.models.{Choice, ObsId, Prefix, RunId}
import csw.messages.params.states.CurrentState

object TromboneHcdState {
  val tromboneConfigFile = new File("trombone/tromboneHCD.conf")
  val resource           = new File("tromboneHCD.conf")

  // HCD Info
  val componentName = "lgsTromboneHCD"
//  val componentType = ComponentType.HCD
  val componentClassName = "csw.examples.vslice.hcd.TromboneHCD"
  val trombonePrefix     = "nfiraos.ncc.tromboneHCD"

  val tromboneAxisName = "tromboneAxis"

  val axisStatePrefix: String      = s"$trombonePrefix.axis1State"
  val axisStateCK: Prefix          = Prefix(axisStatePrefix)
  val axisNameKey: Key[String]     = KeyType.StringKey.make("axisName")
  val AXIS_IDLE: Choice            = Choice(AxisState.AXIS_IDLE.toString)
  val AXIS_MOVING: Choice          = Choice(AxisState.AXIS_MOVING.toString)
  val AXIS_ERROR: Choice           = Choice(AxisState.AXIS_ERROR.toString)
  val stateKey: GChoiceKey         = ChoiceKey.make("axisState", AXIS_IDLE, AXIS_MOVING, AXIS_ERROR)
  val positionKey: Key[Int]        = KeyType.IntKey.make("position")
  val positionUnits: Unit          = encoder
  val inLowLimitKey: Key[Boolean]  = KeyType.BooleanKey.make("lowLimit")
  val inHighLimitKey: Key[Boolean] = KeyType.BooleanKey.make("highLimit")
  val inHomeKey: Key[Boolean]      = KeyType.BooleanKey.make("homed")

  val defaultAxisState: CurrentState = CurrentState(axisStateCK).madd(
    axisNameKey    -> tromboneAxisName,
    stateKey       -> AXIS_IDLE,
    positionKey    -> 0 withUnits encoder,
    inLowLimitKey  -> false,
    inHighLimitKey -> false,
    inHomeKey      -> false
  )

  val axisStatsPrefix           = s"$trombonePrefix.axisStats"
  val axisStatsCK: Prefix       = Prefix(axisStatsPrefix)
  val datumCountKey: Key[Int]   = KeyType.IntKey.make("initCount")
  val moveCountKey: Key[Int]    = KeyType.IntKey.make("moveCount")
  val homeCountKey: Key[Int]    = KeyType.IntKey.make("homeCount")
  val limitCountKey: Key[Int]   = KeyType.IntKey.make("limitCount")
  val successCountKey: Key[Int] = KeyType.IntKey.make("successCount")
  val failureCountKey: Key[Int] = KeyType.IntKey.make("failureCount")
  val cancelCountKey: Key[Int]  = KeyType.IntKey.make("cancelCount")
  val defaultStatsState: CurrentState = CurrentState(axisStatsCK).madd(
    axisNameKey     -> tromboneAxisName,
    datumCountKey   -> 0,
    moveCountKey    -> 0,
    homeCountKey    -> 0,
    limitCountKey   -> 0,
    successCountKey -> 0,
    failureCountKey -> 0,
    cancelCountKey  -> 0
  )

  val axisConfigPrefix     = s"$trombonePrefix.axisConfig"
  val axisConfigCK: Prefix = Prefix(axisConfigPrefix)
  // axisNameKey
  val lowLimitKey: Key[Int]    = KeyType.IntKey.make("lowLimit")
  val lowUserKey: Key[Int]     = KeyType.IntKey.make("lowUser")
  val highUserKey: Key[Int]    = KeyType.IntKey.make("highUser")
  val highLimitKey: Key[Int]   = KeyType.IntKey.make("highLimit")
  val homeValueKey: Key[Int]   = KeyType.IntKey.make("homeValue")
  val startValueKey: Key[Int]  = KeyType.IntKey.make("startValue")
  val stepDelayMSKey: Key[Int] = KeyType.IntKey.make("stepDelayMS")
  // No full default current state because it is determined at runtime
  val defaultConfigState: CurrentState = CurrentState(axisConfigCK).madd(
    axisNameKey -> tromboneAxisName
  )

  val axisMovePrefix     = s"$trombonePrefix.move"
  val axisMoveCK: Prefix = Prefix(axisMovePrefix)

  def positionSC(runId: RunId, obsId: ObsId, value: Int): Setup =
    Setup(runId, obsId, axisMoveCK).add(positionKey -> value withUnits encoder)

  val axisDatumPrefix                     = s"$trombonePrefix.datum"
  val axisDatumCK: Prefix                 = Prefix(axisDatumPrefix)
  def datumSC(runId: RunId, obsId: ObsId) = Setup(runId, obsId, axisDatumCK)

  val axisHomePrefix                     = s"$trombonePrefix.home"
  val axisHomeCK: Prefix                 = Prefix(axisHomePrefix)
  def homeSC(runId: RunId, obsId: ObsId) = Setup(runId, obsId, axisHomeCK)

  val axisCancelPrefix                     = s"$trombonePrefix.cancel"
  val axisCancelCK: Prefix                 = Prefix(axisCancelPrefix)
  def cancelSC(runId: RunId, obsId: ObsId) = Setup(runId, obsId, axisCancelCK)
}
