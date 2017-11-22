package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.messages.framework.ComponentInfo
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, ComponentMessage}
import csw.services.location.scaladsl.LocationService

import scala.concurrent.Future

class ComponentHandlerToSimulateFailure(
    ctx: ActorContext[ComponentMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: LocationService
) extends SampleComponentHandlers(ctx, componentInfo, commandResponseManager, pubSubRef, locationService) {

  override def onShutdown(): Future[Unit] = throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
