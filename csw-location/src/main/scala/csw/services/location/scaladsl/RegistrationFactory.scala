package csw.services.location.scaladsl

import akka.actor.typed.ActorRef
import csw.messages.location.Connection.AkkaConnection
import csw.services.location.models.AkkaRegistration
import csw.services.logging.internal.LogControlMessages

/**
 * `RegistrationFactory` helps creating an AkkaRegistration with the provided `logAdminActorRef`. It is currently used by
 * `csw-framework` to register different components on jvm boot-up. `csw-framework` creates a single `logAdminActorRef`
 * per jvm and injects it in `RegistrationFactory` to register all components with same `logAdminActorRef`.
 *
 * @param logAdminActorRef the ActorRef responsible to change the log level of multiple components started in single
 *                         jvm at runtime
 */
class RegistrationFactory(logAdminActorRef: ActorRef[LogControlMessages]) {

  /**
   * Creates an AkkaRegistration from provided parameters. Currently, it is used to register components except Container
   *
   * @param akkaConnection the AkkaConnection representing the component
   * @param prefix the prefix of the component
   * @param actorRef the supervisor actorref of the component
   * @return a handle to the AkkaRegistration that is used to register in location service
   */
  def akkaTyped(
      akkaConnection: AkkaConnection,
      prefix: String,
      actorRef: ActorRef[_]
  ): AkkaRegistration = AkkaRegistration(akkaConnection, Some(prefix), actorRef, logAdminActorRef)

  /**
   * Creates an AkkaRegistration from provided parameters. Currently, it is used to register Container as it does not
   * have any prefix like other components e.g. Assembly, HCD.
   *
   * @param akkaConnection the AkkaConnection representing the component
   * @param actorRef the supervisor actorref of the component
   * @return a handle to the AkkaRegistration that is used to register in location service
   */
  def akkaTyped(
      akkaConnection: AkkaConnection,
      actorRef: ActorRef[_]
  ): AkkaRegistration = AkkaRegistration(akkaConnection, None, actorRef, logAdminActorRef)
}
