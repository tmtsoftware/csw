package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo

import scala.concurrent.Future

class ComponentHandlerToSimulateFailure(ctx: ActorContext[TopLevelActorMessage],
                                        componentInfo: ComponentInfo,
                                        cswServices: CswServices)
    extends SampleComponentHandlers(ctx, componentInfo, cswServices) {

  override def onShutdown(): Future[Unit] = throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
