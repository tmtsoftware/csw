package csw.config.client.internal

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{actor, Done}

import scala.concurrent.{ExecutionContextExecutor, Future}

/**
 * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
 */
private[csw] class ActorRuntime(_typedSystem: ActorSystem[_] = ActorSystem(SpawnProtocol.behavior, "")) {
  implicit val typedSystem: ActorSystem[_]      = _typedSystem
  implicit val untypedSystem: actor.ActorSystem = typedSystem.toUntyped
  implicit val ec: ExecutionContextExecutor     = typedSystem.executionContext
  implicit val mat: Materializer                = ActorMaterializer()(typedSystem)

  val coordinatedShutdown: CoordinatedShutdown = CoordinatedShutdown(untypedSystem)

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
