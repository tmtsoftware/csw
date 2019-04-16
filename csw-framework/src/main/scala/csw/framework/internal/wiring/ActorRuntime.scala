package csw.framework.internal.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.actor.{typed, ActorSystem, CoordinatedShutdown}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.services.BuildInfo

import scala.concurrent.{ExecutionContextExecutor, Future}

// $COVERAGE-OFF$
/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
private[framework] class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val system: ActorSystem                    = _actorSystem
  implicit val typedActorSystem: typed.ActorSystem[_] = _actorSystem.toTyped
  implicit val ec: ExecutionContextExecutor           = system.dispatcher
  implicit val mat: Materializer                      = ActorMaterializer()

  private[framework] val coordinatedShutdown = CoordinatedShutdown(system)

  def startLogging(name: String): LoggingSystem =
    LoggingSystemFactory.start(name, BuildInfo.version, Networks().hostname, typedActorSystem)

  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
// $COVERAGE-ON$
