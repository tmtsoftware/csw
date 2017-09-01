package csw.common.framework.exceptions

case class TriggerRestartException() extends RuntimeException

case class InitializeFailureStop()    extends RuntimeException
case class InitializeFailureRestart() extends RuntimeException
