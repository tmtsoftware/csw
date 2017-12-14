package csw.messages.location

import java.net.URI

import akka.typed.ActorRef
import csw.messages.ccs.commands.{JWrappedComponent, WrappedComponent}
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.{ComponentMessage, ContainerExternalMessage, TMTSerializable}

import scala.reflect.ClassTag

/**
 * Location represents a live Connection along with its URI
 */
sealed abstract class Location extends TMTSerializable {
  def connection: Connection
  def uri: URI
  def logAdminActorRef: ActorRef[Nothing]
}

// *************** IMPORTANT ***********************
// Do not directly access actorRef from constructor,
// Use one of component(), jComponent() or containerRef() method to get the wrapped component
// and then use wrappedComponent.ref to get correct actor reference.
// *************************************************
/**
 * Represents a live Akka connection of an Actor
 */
final case class AkkaLocation(
    connection: AkkaConnection,
    prefix: Option[String],
    uri: URI,
    actorRef: ActorRef[Nothing],
    logAdminActorRef: ActorRef[Nothing]
) extends Location {

  private def typedRef[T: ClassTag]: ActorRef[T] = {
    val typeManifest    = scala.reflect.classTag[T].runtimeClass.getSimpleName
    val messageManifest = connection.componentId.componentType.messageManifest

    require(typeManifest == messageManifest, s"actorRef for type $messageManifest can not handle messages of type $typeManifest")

    actorRef.upcast[T]
  }

  // If the component type is HCD or Assembly, use this to get the correct ActorRef
  def component(): WrappedComponent   = new WrappedComponent(typedRef[ComponentMessage])
  def jComponent(): JWrappedComponent = new JWrappedComponent(typedRef[ComponentMessage])

  // If the component type is Container, use this to get the correct ActorRef
  def containerRef(): ActorRef[ContainerExternalMessage] = typedRef[ContainerExternalMessage]
}

/**
 * Represents a live Tcp connection
 */
final case class TcpLocation(connection: TcpConnection, uri: URI, logAdminActorRef: ActorRef[Nothing]) extends Location

/**
 * Represents a live Http connection
 */
final case class HttpLocation(connection: HttpConnection, uri: URI, logAdminActorRef: ActorRef[Nothing]) extends Location
