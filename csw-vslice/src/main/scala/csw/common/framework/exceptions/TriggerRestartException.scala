package csw.common.framework.exceptions

case class InitializeFailureStop()    extends RuntimeException
case class InitializeFailureRestart() extends RuntimeException
case class InitializeTimeOut()        extends RuntimeException
