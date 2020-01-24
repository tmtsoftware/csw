package csw.config.client.internal

import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.{Done, actor}

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context and clean up of actor system
 */
private[csw] class ActorRuntime(_typedSystem: ActorSystem[_] = ActorSystem(SpawnProtocol(), "config-client-system")) {
  implicit val typedSystem: ActorSystem[_]  = _typedSystem
  implicit val ec: ExecutionContextExecutor = typedSystem.executionContext

  val classicSystem: actor.ActorSystem = typedSystem.toClassic

  /**
   * The shutdown method helps self node to gracefully quit the akka cluster. It is used by `csw-config-cli`
   * to shutdown the the app gracefully. `csw-config-cli` becomes the part of akka cluster on booting up and
   * resolves the config server, using location service, to provide cli features around admin api of config service.
   *
   * @return a future that completes when shutdown is successful
   */
  def shutdown(): Future[Done] = {
    typedSystem.terminate()
    typedSystem.whenTerminated
  }
}
