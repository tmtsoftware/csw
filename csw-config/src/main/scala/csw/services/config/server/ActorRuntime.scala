package csw.services.config.server

import akka.actor.ActorSystem
import akka.dispatch.MessageDispatcher
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.ExecutionContextExecutor

class ActorRuntime(_actorSystem: ActorSystem, settings: Settings) {
  implicit val actorSystem: ActorSystem = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer = ActorMaterializer()

  val blockingIoDispatcher: MessageDispatcher = actorSystem.dispatchers.lookup(settings.`blocking-io-dispatcher`)
}
