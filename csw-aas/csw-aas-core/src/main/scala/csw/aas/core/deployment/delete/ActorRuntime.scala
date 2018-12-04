//package csw.aas.core.deployment
//
//import akka.Done
//import akka.actor.CoordinatedShutdown.Reason
//import akka.actor.{ActorSystem, CoordinatedShutdown}
//import akka.stream.{ActorMaterializer, Materializer}
//
//import scala.concurrent.{ExecutionContextExecutor, Future}
//
///**
// * A convenient class wrapping actor system and providing handles for execution context, materializer and clean up of actor system
// */
//private[csw] class ActorRuntime(_actorSystem: ActorSystem = ActorSystem()) {
//  implicit val actorSystem: ActorSystem     = _actorSystem
//  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
//  implicit val mat: Materializer            = ActorMaterializer()
//
//  val coordinatedShutdown = CoordinatedShutdown(actorSystem)
//
//  /**
//   * The shutdown method helps self node to gracefully quit the akka cluster. It is used by `csw-config-cli`
//   * to shutdown the the app gracefully. `csw-config-cli` becomes the part of akka cluster on booting up and
//   * resolves the config server, using location service, to provide cli features around admin api of config service.
//   *
//   * @param reason the reason for shutdown
//   * @return a future that completes when shutdown is successful
//   */
//  def shutdown(reason: Reason): Future[Done] = coordinatedShutdown.run(reason)
//}
