package csw.config.client.internal

import akka.{Done, actor}
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context and clean up of actor system
 */
private[csw] class ActorRuntime(_typedSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "config-client-system")) {
  implicit val typedSystem: ActorSystem[_]  = _typedSystem
  implicit val ec: ExecutionContextExecutor = typedSystem.executionContext

  val classicSystem: actor.ActorSystem         = typedSystem.toClassic
  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(classicSystem)

  /**
   * The shutdown method helps self node to gracefully quit the akka cluster. It is used by `csw-config-cli`
   * to shutdown the the app gracefully. `csw-config-cli` becomes the part of akka cluster on booting up and
   * resolves the config server, using location service, to provide cli features around admin api of config service.
   *
   * @param reason the reason for shutdown
   * @return a future that completes when shutdown is successful
   */
  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
}
