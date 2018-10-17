package csw.command.client.models.framework
import csw.serializable.TMTSerializable

/**
 * Lifecycle state of a container actor
 */
sealed trait ContainerLifecycleState extends TMTSerializable

object ContainerLifecycleState {

  /**
   * Represents an idle state of container where it is waiting for all components, that are suppose to run in it, to come up
   */
  case object Idle extends ContainerLifecycleState

  /**
   * Represents a running state of container where all components running in it are up
   */
  case object Running extends ContainerLifecycleState
}
