package csw.common.components.framework

import akka.actor.typed.scaladsl.ActorContext
import csw.framework.models.CswServices
import csw.messages.TopLevelActorMessage

import scala.concurrent.Future

class ComponentHandlerToSimulateFailure(ctx: ActorContext[TopLevelActorMessage], cswServices: CswServices)
    extends SampleComponentHandlers(ctx, cswServices) {

  override def onShutdown(): Future[Unit] = throw new RuntimeException(ComponentHandlerToSimulateFailure.exceptionMsg)
}

object ComponentHandlerToSimulateFailure {
  val exceptionMsg = "Shutdown failure occurred"
}
