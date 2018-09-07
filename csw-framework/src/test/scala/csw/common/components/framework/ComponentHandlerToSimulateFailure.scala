package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswContext
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo

import scala.concurrent.Future

class ComponentHandlerToSimulateFailure(ctx: ActorContext[TopLevelActorMessage], componentInfo: ComponentInfo, cswCtx: CswContext)
    extends SampleComponentHandlers(ctx, componentInfo, cswCtx) {

  override def onShutdown(): Future[Unit] = throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
