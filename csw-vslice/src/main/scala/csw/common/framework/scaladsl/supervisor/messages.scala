package csw.common.framework.scaladsl.supervisor

/**
 * Base trait of supervisor actor messages
 */
sealed trait SupervisorMode

object SupervisorMode {
  case object Running             extends SupervisorMode
  case object LifecycleFailure    extends SupervisorMode
  case object Idle                extends SupervisorMode
  case object PreparingToShutdown extends SupervisorMode
  case object Shutdown            extends SupervisorMode
  case object ShutdownFailure     extends SupervisorMode
}
