package csw.services.admin

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.{ExecutionContextExecutor, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  implicit val actorSystem: ActorSystem     = _actorSystem
  implicit val ec: ExecutionContextExecutor = actorSystem.dispatcher
  implicit val mat: Materializer            = ActorMaterializer()

  val coordinatedShutdown = CoordinatedShutdown(actorSystem)

  def shutdown(): Future[Done] = coordinatedShutdown.run()

}
