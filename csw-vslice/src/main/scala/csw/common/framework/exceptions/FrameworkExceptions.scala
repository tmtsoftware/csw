package csw.common.framework.exceptions

case class FailureStop()       extends RuntimeException
case class FailureRestart()    extends RuntimeException
case class InitializeTimeOut() extends RuntimeException
