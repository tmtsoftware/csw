package csw.services.location.scaladsl

import akka.actor.typed.ActorRef
import csw.services.location.api.models.Connection.AkkaConnection
import csw.services.location.api.models.AkkaRegistration
import csw.messages.params.models.Prefix
import csw.services.logging.messages.LogControlMessages

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
   * Creates an AkkaRegistration from provided parameters. Currently, it is used to register components except Container.
   * A [[csw.services.location.api.exceptions.LocalAkkaActorRegistrationNotAllowed]] can be thrown if the actorRef provided
   * is not a remote actorRef.
   *
   * @param akkaConnection the AkkaConnection representing the component
   * @param prefix the prefix of the component
   * @param actorRef the supervisor actorRef of the component
   * @return a handle to the AkkaRegistration that is used to register in location service
   */
  def akkaTyped(
      akkaConnection: AkkaConnection,
      prefix: Prefix,
      actorRef: ActorRef[_]
  ): AkkaRegistration = AkkaRegistration(akkaConnection, prefix, actorRef, logAdminActorRef)

}
