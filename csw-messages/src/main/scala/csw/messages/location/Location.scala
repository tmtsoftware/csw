package csw.messages.location

import java.net.URI

import acyclic.skipped
import akka.typed
import akka.typed.ActorRef
import csw.messages.TMTSerializable
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}

import scala.reflect.ClassTag

/**
 * Location represents a live Connection along with its URI
 */
sealed abstract class Location extends TMTSerializable {
  def connection: Connection
  def uri: URI
}

/**
 * Represents a live Akka connection of an Actor
 */
final case class AkkaLocation(
    connection: AkkaConnection,
    uri: URI,
    actorRef: ActorRef[Nothing],
    adminActorRef: ActorRef[_]
) extends Location {

  def typedRef[T: ClassTag]: typed.ActorRef[T] = {
    val typeManifest    = scala.reflect.classTag[T].runtimeClass.getSimpleName
    val messageManifest = connection.componentId.componentType.messageManifest

    require(typeManifest == messageManifest,
            s"actorRef for type $messageManifest can not handle messages of type $typeManifest")

    actorRef.asInstanceOf[ActorRef[T]]
  }
}

/**
 * Represents a live Tcp connection
 */
final case class TcpLocation(connection: TcpConnection, uri: URI) extends Location

/**
 * Represents a live Http connection
 */
final case class HttpLocation(connection: HttpConnection, uri: URI) extends Location
