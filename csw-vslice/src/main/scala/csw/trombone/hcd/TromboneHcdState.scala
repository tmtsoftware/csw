package csw.trombone.hcd

import java.io.File

import csw.param.Parameters.{CommandInfo, Prefix, Setup}
import csw.param.StateVariable.CurrentState
import csw.param.UnitsOfMeasure.encoder
import csw.param._
import csw.param.parameters.{Choice, ChoiceKey, Keys}
import csw.param.parameters.primitives.StringKey

object TromboneHcdState {
  val tromboneConfigFile = new File("trombone/tromboneHCD.conf")
  val resource           = new File("tromboneHCD.conf")

  // HCD Info
  val componentName = "lgsTromboneHCD"
//  val componentType = ComponentType.HCD
  val componentClassName = "csw.examples.vslice.hcd.TromboneHCD"
  val trombonePrefix     = "nfiraos.ncc.tromboneHCD"

  val tromboneAxisName = "tromboneAxis"

  val axisStatePrefix             = s"$trombonePrefix.axis1State"
  val axisStateCK: Prefix         = axisStatePrefix
  val axisNameKey                 = StringKey("axisName")
  val AXIS_IDLE                   = Choice(AxisState.AXIS_IDLE.toString)
  val AXIS_MOVING                 = Choice(AxisState.AXIS_MOVING.toString)
  val AXIS_ERROR                  = Choice(AxisState.AXIS_ERROR.toString)
  val stateKey                    = ChoiceKey("axisState", AXIS_IDLE, AXIS_MOVING, AXIS_ERROR)
  val positionKey                 = Keys.IntegerKey.make("position")
  val positionUnits: encoder.type = encoder
  val inLowLimitKey               = Keys.BooleanKey.make("lowLimit")
  val inHighLimitKey              = Keys.BooleanKey.make("highLimit")
  val inHomeKey                   = Keys.BooleanKey.make("homed")

  val defaultAxisState: StateVariable.CurrentState = CurrentState(axisStateCK).madd(
    axisNameKey    -> tromboneAxisName,
    stateKey       -> AXIS_IDLE,
    positionKey    -> 0 withUnits encoder,
    inLowLimitKey  -> false,
    inHighLimitKey -> false,
    inHomeKey      -> false
  )

  val axisStatsPrefix     = s"$trombonePrefix.axisStats"
  val axisStatsCK: Prefix = axisStatsPrefix
  val datumCountKey       = Keys.IntegerKey.make("initCount")
  val moveCountKey        = Keys.IntegerKey.make("moveCount")
  val homeCountKey        = Keys.IntegerKey.make("homeCount")
  val limitCountKey       = Keys.IntegerKey.make("limitCount")
  val successCountKey     = Keys.IntegerKey.make("successCount")
  val failureCountKey     = Keys.IntegerKey.make("failureCount")
  val cancelCountKey      = Keys.IntegerKey.make("cancelCount")
  val defaultStatsState: StateVariable.CurrentState = CurrentState(axisStatsCK).madd(
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
  val axisConfigCK: Prefix = axisConfigPrefix
  // axisNameKey
  val lowLimitKey    = Keys.IntegerKey.make("lowLimit")
  val lowUserKey     = Keys.IntegerKey.make("lowUser")
  val highUserKey    = Keys.IntegerKey.make("highUser")
  val highLimitKey   = Keys.IntegerKey.make("highLimit")
  val homeValueKey   = Keys.IntegerKey.make("homeValue")
  val startValueKey  = Keys.IntegerKey.make("startValue")
  val stepDelayMSKey = Keys.IntegerKey.make("stepDelayMS")
  // No full default current state because it is determined at runtime
  val defaultConfigState: StateVariable.CurrentState = CurrentState(axisConfigCK).madd(
    axisNameKey -> tromboneAxisName
  )

  val axisMovePrefix     = s"$trombonePrefix.move"
  val axisMoveCK: Prefix = axisMovePrefix

  def positionSC(commandInfo: CommandInfo, value: Int): Setup =
    Setup(commandInfo, axisMoveCK).add(positionKey -> value withUnits encoder)

  val axisDatumPrefix                   = s"$trombonePrefix.datum"
  val axisDatumCK: Prefix               = axisDatumPrefix
  def datumSC(commandInfo: CommandInfo) = Setup(commandInfo, axisDatumCK)

  val axisHomePrefix                   = s"$trombonePrefix.home"
  val axisHomeCK: Prefix               = axisHomePrefix
  def homeSC(commandInfo: CommandInfo) = Setup(commandInfo, axisHomeCK)

  val axisCancelPrefix                   = s"$trombonePrefix.cancel"
  val axisCancelCK: Prefix               = axisCancelPrefix
  def cancelSC(commandInfo: CommandInfo) = Setup(commandInfo, axisCancelCK)
}
