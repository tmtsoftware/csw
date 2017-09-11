package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.PubSub.PublisherMessage
import csw.common.framework.models.{ComponentInfo, ComponentMessage}
import csw.param.states.CurrentState

import scala.concurrent.Future

class ComponentHandlerToSimulateFailure(ctx: ActorContext[ComponentMessage],
                                        componentInfo: ComponentInfo,
                                        pubSubRef: ActorRef[PublisherMessage[CurrentState]])
    extends SampleComponentHandlers(ctx, componentInfo, pubSubRef) {

  override def onShutdown(): Future[Unit] = {
    throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
  }
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
