package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswContext
import csw.command.client.messages.TopLevelActorMessage

import scala.concurrent.Future

class ComponentHandlerToSimulateFailure(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends SampleComponentHandlers(ctx, cswCtx) {

  override def onShutdown(): Future[Unit] = throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
