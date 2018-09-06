package csw.services.command.extensions

import akka.actor.typed.ActorRef
import csw.messages.{ComponentMessage, ContainerMessage}
import csw.messages.location.AkkaLocation

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
