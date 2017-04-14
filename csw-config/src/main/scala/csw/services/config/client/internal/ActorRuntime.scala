package csw.services.config.client.internal

import akka.actor.{ActorSystem, Terminated}
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem = ActorSystem()) {
  implicit val actorSystem: ActorSystem = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer = ActorMaterializer()

  def shutdown(): Future[Terminated] = actorSystem.terminate()
}
