package csw.messages.location

import java.net.URI

import akka.typed.ActorRef
import csw.messages.location.Connection.{AkkaConnection, HttpConnection, TcpConnection}
import csw.messages.scaladsl.{ComponentMessage, ContainerMessage}
import csw.messages.TMTSerializable

import scala.reflect.ClassTag

/**
 * Location represents a live Connection along with its URI
 */
sealed abstract class Location extends TMTSerializable {

  /**
   * Represents a connection based on a componentId and the type of connection offered by the component
   */
  def connection: Connection

  /**
   * Represents the URI of the component
   */
  def uri: URI

  /**
   * The LogAdminActorRef for this component that handles dynamic log level changes for that component
   */
  def logAdminActorRef: ActorRef[Nothing]
}

//*************** IMPORTANT ***********************
//Do not directly access actorRef from constructor,
//Use one of component(), jComponent() or containerRef() method to get the wrapped component
//and then use wrappedComponent.ref to get correct actor reference.
//*************************************************
/**
 * Represents a live Akka connection of an Actor
 *
 * @note Do not directly access actorRef from constructor, sse one of component(), jComponent() or containerRef() method
 *       to get the wrapped component and then use wrappedComponent.ref to get correct actor reference.
 * @param connection represents a connection based on a componentId and the type of connection offered by the component
 * @param maybePrefix an optional prefix of the component. In case of container this option remains none.
 * @param uri represents the URI of the component. URI is not significant for AkkaLocation as actorRef serves the purpose
 *            of exposed remote address of component.
 * @param actorRef gateway or router for a component that other components will resolve and talk to
 * @param logAdminActorRef handles dynamic log level changes for that component
 */
final case class AkkaLocation(
    connection: AkkaConnection,
    maybePrefix: Option[String],
    uri: URI,
    actorRef: ActorRef[Nothing],
    logAdminActorRef: ActorRef[Nothing]
) extends Location {

  // Akka typed actors currently don't save the type while sending ActorRef on wire.
  // So, while resolving any ActorRef for component cast the untyped ActorRef to typed one
  private def typedRef[T: ClassTag]: ActorRef[T] = {
    val typeManifest    = scala.reflect.classTag[T].runtimeClass.getSimpleName
    val messageManifest = connection.componentId.componentType.messageManifest

    require(typeManifest == messageManifest, s"actorRef for type $messageManifest can not handle messages of type $typeManifest")

    actorRef.upcast[T]
  }

  /**
   * If the component type is HCD or Assembly, use this to get the correct ActorRef
   *
   * @return a typed ActorRef that understands only ComponentMessage
   */
  def componentRef: ActorRef[ComponentMessage] = typedRef[ComponentMessage]

  /**
   * If the component type is Container, use this to get the correct ActorRef
   *
   * @return a typed ActorRef that understands only ContainerMessage
   */
  def containerRef: ActorRef[ContainerMessage] = typedRef[ContainerMessage]
}

/**
 * Represents a live Tcp connection
 *
 * @param connection represents a connection based on a componentId and the type of connection offered by the component
 * @param uri represents the remote URI of the component that other components will resolve and talk to
 * @param logAdminActorRef handles dynamic log level changes for that component
 */
final case class TcpLocation(connection: TcpConnection, uri: URI, logAdminActorRef: ActorRef[Nothing]) extends Location

/**
 * Represents a live Http connection
 *
 * @param connection represents a connection based on a componentId and the type of connection offered by the component
 * @param uri represents the remote URI of the component that other components will resolve and talk to
 * @param logAdminActorRef handles dynamic log level changes for that component
 */
final case class HttpLocation(connection: HttpConnection, uri: URI, logAdminActorRef: ActorRef[Nothing]) extends Location
