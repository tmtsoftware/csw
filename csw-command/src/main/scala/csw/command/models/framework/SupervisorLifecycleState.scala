package csw.command.models.framework

import csw.params.TMTSerializable

/**
 * Lifecycle state of a Supervisor actor
 */
sealed trait SupervisorLifecycleState extends TMTSerializable

object SupervisorLifecycleState {

  /**
   * Represents an idle state of a component where it is waiting for TLA to initialize
   */
  case object Idle extends SupervisorLifecycleState

  /**
   * Represents a running state of component where it is initialized, registered with location service and waiting for commands
   * from other components
   */
  case object Running extends SupervisorLifecycleState

  /**
   * Represents a running state of component but in offline mode
   */
  case object RunningOffline extends SupervisorLifecycleState

  /**
   * Represents a restarting state of a component. When a component receives a Restart message it unregisters itself from location
   * service, restart the underlying TLA, waits for its initialization and registers again with location service.
   */
  case object Restart extends SupervisorLifecycleState

  /**
   * Represents a shutting down state of a component. When a component receives a Shutdown message it stops the underlying TLA
   * and other child actors, then it gracefully shuts down the ActorSystem for this component and unregisters itself from
   * location service.
   */
  case object Shutdown extends SupervisorLifecycleState

  /**
   * Represents a locked state of a component
   */
  case object Lock extends SupervisorLifecycleState
}
