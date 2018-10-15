package csw.command.client.internal.extensions

import akka.actor.typed.ActorRef
import csw.command.client.internal.messages.{ComponentMessage, ContainerMessage}
import csw.location.api.models.AkkaLocation

//TODO: Find a better way for Java Support
object AkkaLocationExt {
  implicit class RichAkkaLocation(val akkaLocation: AkkaLocation) {

    /**
     * If the component type is HCD or Assembly, use this to get the correct ActorRef
     *
     * @return a typed ActorRef that understands only ComponentMessage
     */
    def componentRef: ActorRef[ComponentMessage] = akkaLocation.typedRef[ComponentMessage]

    /**
     * If the component type is Container, use this to get the correct ActorRef
     *
     * @return a typed ActorRef that understands only ContainerMessage
     */
    def containerRef: ActorRef[ContainerMessage] = akkaLocation.typedRef[ContainerMessage]

  }

}
