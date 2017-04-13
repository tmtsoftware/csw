package csw.services.config.client.internal

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.ExecutionContextExecutor

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer = ActorMaterializer()
}
