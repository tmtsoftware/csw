package csw.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.typed.ActorRef
import akka.typed.javadsl.ActorContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.framework.ComponentInfo
import csw.messages.models.PubSub.PublisherMessage
import csw.messages.params.states.CurrentState
import csw.messages.{CommandResponseManagerMessage, TopLevelActorMessage}
import csw.services.location.javadsl.ILocationService
import csw.services.logging.javadsl.JLoggerFactory

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Base class for component handlers which will be used by the component actor
 * @param ctx               The Actor Context under which the actor instance of the component, which use these handlers, is created
 * @param componentInfo     Component related information as described in the configuration file
 * @param pubSubRef         The pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]] for this component
 * @param locationService   The single instance of Location service created for a running application
 */
abstract class JComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    commandResponseManager: ActorRef[CommandResponseManagerMessage],
    pubSubRef: ActorRef[PublisherMessage[CurrentState]],
    locationService: ILocationService,
    loggerFactory: JLoggerFactory
) extends ComponentHandlers(
      ctx.asScala,
      componentInfo,
      commandResponseManager,
      pubSubRef,
      locationService.asScala,
      loggerFactory.asScala
    ) {

  implicit val ec: ExecutionContextExecutor = ctx.getExecutionContext

  def jInitialize(): CompletableFuture[Unit]
  def jOnShutdown(): CompletableFuture[Unit]

  // do not override this from java class
  override def initialize(): Future[Unit] = jInitialize().toScala

  // do not override this from java class
  override def onShutdown(): Future[Unit] = jOnShutdown().toScala
}
