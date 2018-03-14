package csw.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.typed.javadsl.ActorContext
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
 *
 * @param ctx the ActorContext under which the actor instance of the component, which use these handlers, is created
 * @param componentInfo component related information as described in the configuration file
 * @param currentStatePublisher the pub sub actor to publish state represented by [[csw.messages.params.states.CurrentState]] for this component
 * @param locationService the single instance of Location service created for a running application
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

  /**
   * A Java helper that is invoked when the component is created. This is different than constructor initialization
   * to allow non-blocking asynchronous operations. The component can initialize state such as configuration to be fetched
   * from configuration service, location of components or services to be fetched from location service etc. These vary
   * from component to component.
   *
   * @return a CompletableFuture which completes when the initialization of component completes
   */
  def jInitialize(): CompletableFuture[Void]

  /**
   * The onShutdown handler can be used for carrying out the tasks which will allow the component to shutdown gracefully
   *
   * @return a CompletableFuture which completes when the shutdown completes for component
   */
  def jOnShutdown(): CompletableFuture[Void]

  /**
   * Invokes the java helper (jInitialize) to initialize the component
   *
   * @note do not override this from java class
   * @return a future which completes when jInitialize completes
   */
  override def initialize(): Future[Unit] = jInitialize().toScala.map(_ ⇒ Unit)

  /**
   * Invokes the java helper (jOnShutdown) to shutdown the component
   *
   * @note do not override this from java class
   * @return a future which completes when the jOnShutdown completes
   */
  override def onShutdown(): Future[Unit] = jOnShutdown().toScala.map(_ ⇒ Unit)
}
