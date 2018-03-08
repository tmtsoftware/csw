package csw.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.javadsl.ActorContext
import csw.framework.scaladsl.{ComponentHandlers, CurrentStatePublisher}
import csw.messages.framework.ComponentInfo
import csw.messages.scaladsl.TopLevelActorMessage
import csw.services.command.scaladsl.CommandResponseManager
import csw.services.location.javadsl.ILocationService
import csw.services.logging.javadsl.JLoggerFactory

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Base class for component handlers which will be used by the component actor
 * @param ctx                    The Actor Context under which the actor instance of the component, which use these handlers, is created
 * @param componentInfo          Component related information as described in the configuration file
 * @param currentStatePublisher  The pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]] for this component
 * @param locationService        The single instance of Location service created for a running application
 */
abstract class JComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: CommandResponseManager,
    currentStatePublisher: CurrentStatePublisher,
    locationService: ILocationService,
    loggerFactory: JLoggerFactory
) extends ComponentHandlers(
      ctx.asScala,
      componentInfo,
      commandResponseManager,
      currentStatePublisher,
      locationService.asScala,
      loggerFactory.asScala
    ) {

  implicit val ec: ExecutionContextExecutor = ctx.getExecutionContext

  def jInitialize(): CompletableFuture[Void]
  def jOnShutdown(): CompletableFuture[Void]

  // do not override this from java class
  override def initialize(): Future[Unit] = jInitialize().toScala.map(_ ⇒ Unit)

  // do not override this from java class
  override def onShutdown(): Future[Unit] = jOnShutdown().toScala.map(_ ⇒ Unit)
}
