package csw.command.client.messages

import akka.actor.typed.ActorRef
import csw.command.client.messages.CommandSerializationMarker._
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.models.framework.PubSub.SubscriberMessage
import csw.command.client.models.framework._
import csw.location.api.models.TrackingEvent
import csw.logging.models.{Level, LogMetadata}
import csw.params.commands.CommandResponse._
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.CurrentState
import csw.prefix.models.Prefix
import csw.serializable.CommandSerializable
import csw.time.core.models.UTCTime

import scala.concurrent.duration.FiniteDuration

object CommandSerializationMarker {
  sealed trait RemoteMsg extends CommandSerializable
}

/**
 * Represents messages received by TopLevelActor e.g Lifecycle(GoOffline), Submit(Setup, replyTo), etc.
 */
sealed trait TopLevelActorMessage

private[csw] sealed trait TopLevelActorCommonMessage extends TopLevelActorMessage
private[csw] object TopLevelActorCommonMessage {
  case class UnderlyingHookFailed(throwable: Throwable)          extends TopLevelActorCommonMessage
  case class TrackingEventReceived(trackingEvent: TrackingEvent) extends TopLevelActorCommonMessage
}

private[csw] sealed trait TopLevelActorIdleMessage extends TopLevelActorMessage
private[csw] object TopLevelActorIdleMessage {
  case object Initialize extends TopLevelActorIdleMessage
}

/**
 * Represent messages that carry commands sent from one component to other
 */
sealed trait CommandMessage extends RunningMessage with SupervisorLockMessage {

  /**
   * Represents a command sent to other component
   */
  def command: ControlCommand

  /**
   * Represents the actor that will receive the Locked response for Submit, Oneway and Validate kind of commands
   */
  def replyTo: ActorRef[Locked]
}

object CommandMessage {

  /**
   * Represents a submit kind of message that carries command to other component
   *
   * @param command represents a command sent to other component
   * @param replyTo represents the actor that will receive the command response
   */
  case class Submit(command: ControlCommand, replyTo: ActorRef[SubmitResponse]) extends CommandMessage

  /**
   * Represents a oneway kind of message that carries command to other component
   *
   * @param command represents a command sent to other component
   * @param replyTo represents the actor that will receive the command response
   */
  case class Oneway(command: ControlCommand, replyTo: ActorRef[OnewayResponse]) extends CommandMessage

  /**
   * Represents a validate only kind of message that carries command to other component
   *
   * @param command represents a command sent to other component
   * @param replyTo represents the actor that will receive the command response
   */
  case class Validate(command: ControlCommand, replyTo: ActorRef[ValidateResponse]) extends CommandMessage
}

private[csw] case class LockTimedout(replyTo: ActorRef[LockingResponse])       extends SupervisorMessage
private[csw] case class LockAboutToTimeout(replyTo: ActorRef[LockingResponse]) extends SupervisorMessage

/**
 * Represents messages regarding locking and un-locking a component and messages that can be received when a component is
 * locked
 */
sealed trait SupervisorLockMessage extends SupervisorRunningMessage with RemoteMsg
object SupervisorLockMessage {

  /**
   * Represents message to lock a component
   *
   * @param source represents the prefix of component that is acquiring lock
   * @param replyTo represents the actor that will receive the command response
   * @param leaseDuration represents the lease duration of lock acquired
   */
  case class Lock(source: Prefix, replyTo: ActorRef[LockingResponse], leaseDuration: FiniteDuration) extends SupervisorLockMessage

  /**
   * Represents message to un-lock an already locked component
   *
   * @param source represents the prefix of component that is acquiring lock
   * @param replyTo represents the actor that will receive the command response
   */
  case class Unlock(source: Prefix, replyTo: ActorRef[LockingResponse]) extends SupervisorLockMessage
}

/**
 * Represents messages that a component will receive in running state
 */
sealed trait RunningMessage extends TopLevelActorMessage with SupervisorRunningMessage with RemoteMsg
object RunningMessage {

  /**
   * Represents a transition in lifecycle state of a component
   *
   * @param message represents the command a component should honour and transit itself to a new lifecycle state
   *                e.g. GoOffline or GoOnline
   */
  case class Lifecycle(message: ToComponentLifecycleMessage) extends RunningMessage with ContainerMessage
}

sealed trait DiagnosticDataMessage extends RunningMessage

object DiagnosticDataMessage {
  case class DiagnosticMode(startTime: UTCTime, hint: String) extends DiagnosticDataMessage
  case object OperationsMode                                  extends DiagnosticDataMessage
}

/**
 * Represents shutdown or restart kind of messages sent to a component
 */
sealed trait CommonMessage extends ComponentCommonMessage with ContainerCommonMessage

object SupervisorContainerCommonMessages {

  /**
   * A Java helper that represents a message for a component. When received, component takes necessary clean up action and unregisters
   * itself with location service. If the component is a container or run as a standalone process, then shutdown will also
   * kill the jvm process it is running in.
   */
  def jShutdown(): CommonMessage = Shutdown

  /**
   * A Java helper that represents a restart message for a component
   */
  def jRestart(): CommonMessage = Restart

  /**
   * Represents a shutdown message for a component. When received, component takes necessary clean up action and unregisters
   * itself with location service. If the component is a container or run as a standalone process, then shutdown will also
   * kill the jvm process it is running in.
   */
  case object Shutdown extends CommonMessage

  /**
   * Represents a restart message for a component
   */
  case object Restart extends CommonMessage
}
////////////////////

private[csw] sealed trait SupervisorMessage

/**
 * Represents messages that a component can receive in it's whole lifecycle
 */
sealed trait ComponentMessage extends SupervisorMessage

/**
 * Represents messages that a component can receive in running state
 */
sealed trait SupervisorRunningMessage extends ComponentMessage

private[csw] sealed trait SupervisorInternalRunningMessage extends SupervisorMessage
private[csw] object SupervisorInternalRunningMessage {
  case class RegistrationSuccess(componentRef: ActorRef[RunningMessage])     extends SupervisorInternalRunningMessage
  case class RegistrationNotRequired(componentRef: ActorRef[RunningMessage]) extends SupervisorInternalRunningMessage
  case class RegistrationFailed(throwable: Throwable)                        extends SupervisorInternalRunningMessage
}

private[csw] sealed trait SupervisorRestartMessage extends SupervisorMessage
private[csw] object SupervisorRestartMessage {
  case class UnRegistrationFailed(throwable: Throwable) extends SupervisorRestartMessage

  case object UnRegistrationComplete extends SupervisorRestartMessage
}

/**
 * Represents messages that a component can receive in any state
 */
sealed trait ComponentCommonMessage extends ComponentMessage with RemoteMsg
object ComponentCommonMessage {

  /**
   * Represents a message to create subscription for lifecycle changes of a component
   *
   * @param subscriberMessage tells the component to subscribe to or unsubscribe from LifecycleStateChanged notifications
   */
  case class LifecycleStateSubscription(subscriberMessage: SubscriberMessage[LifecycleStateChanged])
      extends ComponentCommonMessage

  /**
   * Represents a message to create subscription for state changes of a component
   *
   * @param subscriberMessage tells the component to subscribe to or unsubscribe from CurrentState notifications
   */
  case class ComponentStateSubscription(subscriberMessage: SubscriberMessage[CurrentState]) extends ComponentCommonMessage

  /**
   * Represents a message to get current lifecycle state of a component
   *
   * @param replyTo an ActorRef that will receive SupervisorLifecycleState
   */
  case class GetSupervisorLifecycleState(replyTo: ActorRef[SupervisorLifecycleState]) extends ComponentCommonMessage
}

private[csw] sealed trait SupervisorIdleMessage extends SupervisorMessage
private[csw] object SupervisorIdleMessage {
  case object InitializeTimeout extends SupervisorIdleMessage
}

private[csw] sealed trait FromComponentLifecycleMessage extends SupervisorIdleMessage with SupervisorRunningMessage
private[csw] object FromComponentLifecycleMessage {
  case class Running(componentRef: ActorRef[RunningMessage]) extends FromComponentLifecycleMessage
}

///////////////////
private[csw] sealed trait ContainerActorMessage

/**
 * Represents messages a container can receive in it's whole lifecycle
 */
sealed trait ContainerMessage extends ContainerActorMessage with RemoteMsg

/**
 * Represents messages a container can receive in any state
 */
sealed trait ContainerCommonMessage extends ContainerMessage
object ContainerCommonMessage {

  /**
   * Represents a message to get all components started in a container
   *
   * @param replyTo represents the actor that will receive a set of components
   */
  case class GetComponents(replyTo: ActorRef[Components]) extends ContainerCommonMessage

  /**
   * Represents a message to get lifecycle state a container
   *
   * @param replyTo represents the actor that will receive lifecycle state of a container
   */
  case class GetContainerLifecycleState(replyTo: ActorRef[ContainerLifecycleState]) extends ContainerCommonMessage
}

private[csw] sealed trait ContainerIdleMessage extends ContainerActorMessage
private[csw] object ContainerIdleMessage {
  case class SupervisorsCreated(supervisors: Set[SupervisorInfo]) extends ContainerIdleMessage
}

private[csw] sealed trait FromSupervisorMessage extends ContainerIdleMessage
private[csw] object FromSupervisorMessage {
  case class SupervisorLifecycleStateChanged(
      supervisor: ActorRef[ComponentMessage],
      supervisorLifecycleState: SupervisorLifecycleState
  ) extends FromSupervisorMessage
}

////////////////

/**
 * Represents a message to query the command status of a command running on some component
 *
 * @param runId represents an unique identifier of command
 * @param replyTo represents the actor that will receive the command status
 */
case class Query(runId: Id, replyTo: ActorRef[SubmitResponse]) extends SupervisorLockMessage

/**
 * Represents a message to subscribe to change in command status of a command running on some component
 *
 * @param runId represents an unique identifier of command
 * @param replyTo represents the actor that will receive the notification of change in command status
 */
case class QueryFinal(runId: Id, replyTo: ActorRef[SubmitResponse]) extends SupervisorLockMessage

// Parent trait for Messages which will be send to components for interacting with its logging system
sealed trait LogControlMessage extends ComponentMessage with SequencerMsg with RemoteMsg

// Message to get Logging configuration metadata of the receiver
case class GetComponentLogMetadata(replyTo: ActorRef[LogMetadata]) extends LogControlMessage

// Message to change the log level of any component
case class SetComponentLogLevel(logLevel: Level) extends LogControlMessage
