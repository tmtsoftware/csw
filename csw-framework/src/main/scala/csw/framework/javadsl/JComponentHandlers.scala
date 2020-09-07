package csw.framework.javadsl

import akka.actor.typed.javadsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.JCswContext
import csw.framework.scaladsl.ComponentHandlers

import scala.concurrent.ExecutionContextExecutor

/**
 * Base class for component handlers which will be used by the component actor
 *
 * @param ctx the [[akka.actor.typed.javadsl.ActorContext]] under which the actor instance of the component, which use these handlers, is created
 * @param cswCtx provides access to csw services e.g. location, event, alarm, etc
 */
abstract class JComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: JCswContext)
    extends ComponentHandlers(ctx.asScala, cswCtx.asScala) {

  implicit val ec: ExecutionContextExecutor = ctx.getExecutionContext

  /**
   * A Java helper that is invoked when the component is created. The component can initialize state such as configuration to be fetched
   * from configuration service, location of components or services to be fetched from location service etc. These vary
   * from component to component.
   *
   * @return completes when the initialization of component completes
   */
  def jInitialize(): Unit

  /**
   * The onShutdown handler can be used for carrying out the tasks which will allow the component to shutdown gracefully
   *
   * @return when the shutdown completes for component
   */
  def jOnShutdown(): Unit

  /**
   * Invokes the java helper (jInitialize) to initialize the component
   *
   * Note: do not override this from java class
   *
   * @return a future which completes when jInitialize completes
   */
  override def initialize(): Unit = jInitialize()

  /**
   * Invokes the java helper (jOnShutdown) to shutdown the component
   *
   * Note: do not override this from java class
   */
  override def onShutdown(): Unit = jOnShutdown()
}
