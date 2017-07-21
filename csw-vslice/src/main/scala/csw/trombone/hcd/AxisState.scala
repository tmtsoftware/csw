package csw.trombone.hcd

sealed trait AxisState

object AxisState {
  case object AXIS_IDLE   extends AxisState
  case object AXIS_MOVING extends AxisState
  case object AXIS_ERROR  extends AxisState
}
