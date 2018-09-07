package csw.framework.javadsl

import java.util.concurrent.CompletableFuture

import akka.actor.typed.javadsl.ActorContext
import csw.framework.CurrentStatePublisher
import csw.framework.models.JCswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.messages.TopLevelActorMessage
import csw.messages.framework.ComponentInfo
import csw.services.command.CommandResponseManager

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * Base class for component handlers which will be used by the component actor
 *
 * @param ctx the [[akka.actor.typed.javadsl.ActorContext]] under which the actor instance of the component, which use these handlers, is created
 * @param componentInfo component related information as described in the configuration file
 * @param cswCtx provides access to csw services e.g. location, event, alarm, etc
 */
abstract class JComponentHandlers(
    ctx: ActorContext[TopLevelActorMessage],
    componentInfo: ComponentInfo,
    cswCtx: JCswContext
) extends ComponentHandlers(
      ctx.asScala,
      componentInfo,
      cswCtx.asScala
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
