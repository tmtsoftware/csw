package csw.common.components

import akka.typed.ActorRef
import akka.typed.scaladsl.ActorContext
import csw.common.framework.models.PubSub.PublisherMsg
import csw.common.framework.models.{ComponentInfo, ComponentMsg}
import csw.param.states.CurrentState

class ComponentHandlerToSimulateFailure(ctx: ActorContext[ComponentMsg],
                                        componentInfo: ComponentInfo,
                                        pubSubRef: ActorRef[PublisherMsg[CurrentState]])
    extends SampleComponentHandlers(ctx, componentInfo, pubSubRef) {

  override def onShutdown(): Unit = {
    throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
  }
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
