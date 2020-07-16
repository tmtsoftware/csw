package csw.common.components.framework

import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.{ComponentContext, CswContext}

import scala.concurrent.Future

class ComponentHandlerToSimulateFailure(ctx: ComponentContext[TopLevelActorMessage], cswCtx: CswContext)
    extends SampleComponentHandlers(ctx, cswCtx) {

  override def onShutdown(): Future[Unit] = throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
