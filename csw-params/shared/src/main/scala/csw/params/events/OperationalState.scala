package csw.params.events

sealed trait OperationalState

object OperationalState {
  case object READY extends OperationalState
  case object ERROR extends OperationalState
  case object BUSY  extends OperationalState
}

object JOperationalState {
  val READY = OperationalState.READY
  val ERROR = OperationalState.ERROR
  val BUSY  = OperationalState.BUSY
}
