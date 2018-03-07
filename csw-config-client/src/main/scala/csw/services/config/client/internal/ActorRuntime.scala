package csw.services.config.client.internal

import akka.Done
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown.Reason
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.location.commons.CswCoordinatedShutdown

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
class ActorRuntime private[csw] (_actorSystem: ActorSystem = ActorSystem()) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  /**
   * The shutdown method helps self node to gracefully quit the akka cluster. It is used by `csw-config-client-cli`
   * to shutdown the the app gracefully. `csw-config-client-cli` becomes the part of akka cluster on booting up and
   * resolves the config server, using location service, to provide cli features around admin api of config service.
   *
   * @param reason the reason for shutdown
   * @return a future that completes when shutdown is successful
   */
  def shutdown(reason: Reason): Future[Done] = CswCoordinatedShutdown.run(actorSystem, reason)
}
