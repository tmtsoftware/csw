package csw.services.location.api.exceptions

import akka.actor.typed.ActorRef
import csw.messages.location.{Connection, Location}

/**
 * An Exception representing failure in registration
 *
 */
case class RegistrationFailed(msg: String) extends RuntimeException(msg) {
  def this(connection: Connection) = this(s"unable to register $connection")
}

/**
 * An Exception representing failure in un-registration
 *
 * @param connection a connection for which un-registration failed
 */
case class UnregistrationFailed(connection: Connection) extends RuntimeException(s"unable to unregister $connection")

/**
 * An Exception representing failure in registration as other location is already registered in place of the given location
 *
 */
case class OtherLocationIsRegistered(msg: String) extends RuntimeException(msg) {
  def this(location: Location, otherLocation: Location) =
    this(s"there is other location=$otherLocation registered against name=${location.connection.name}.")
}

/**
 * An Exception representing failure in registering non remote actors
 *
 * @param actorRef the reference of the Actor that is expected to be remote but instead it is local
 */
case class LocalAkkaActorRegistrationNotAllowed(actorRef: ActorRef[_])
    extends RuntimeException(s"Registration of only remote actors is allowed. Instead local actor $actorRef received.")

/**
 * An Exception representing failure in listing locations
 */
case object RegistrationListingFailed extends RuntimeException(s"unable to get the list of registered locations")

/**
 * Represents if the distributed data is not confirmed to be replicated on current node
 */
case object CouldNotEnsureDataReplication
    extends RuntimeException("could not ensure that the data is replicated in location service cluster")

/**
 * Represents the current node is not able to join the cluster
 */
case object CouldNotJoinCluster extends RuntimeException("could not join cluster")

/**
 * ClusterSeedsNotFound is thrown while creating location service remote http client if clusterSeeds are not provided or set in
 * as environment variable
 */
case object ClusterSeedsNotFound
    extends RuntimeException(
      "clusterSeeds setting is not specified either as env variable or system property. " +
      "Please check online documentation for this set-up."
    )
