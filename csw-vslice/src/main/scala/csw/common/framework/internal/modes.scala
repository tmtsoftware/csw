package csw.common.framework.internal

sealed trait ComponentMode
object ComponentMode {
  case object Idle        extends ComponentMode
  case object Initialized extends ComponentMode
  case object Running     extends ComponentMode
}

sealed trait SupervisorMode
object SupervisorMode {
  case object Idle                extends SupervisorMode
  case object InitializeFailure   extends SupervisorMode
  case object Running             extends SupervisorMode
  case object RunningOffline      extends SupervisorMode
  case object PreparingToShutdown extends SupervisorMode
  case object Shutdown            extends SupervisorMode
  case object ShutdownFailure     extends SupervisorMode
}

sealed trait ContainerMode
object ContainerMode {
  case object Idle    extends ContainerMode
  case object Running extends ContainerMode
}
