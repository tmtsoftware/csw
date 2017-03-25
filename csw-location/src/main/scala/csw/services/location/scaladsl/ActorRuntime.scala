package csw.services.location.scaladsl

import akka.actor.{ActorRef, ActorSystem, Terminated}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import csw.services.location.internal.Settings

import scala.concurrent.duration.DurationLong
import scala.concurrent.{ExecutionContext, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  def this(name: String, settings: Settings) = this(ActorSystem(name, settings.config(name)))
  def this(name: String) = this(name, Settings())

  val hostname: String = _actorSystem.settings.config.getString("akka.remote.netty.tcp.hostname")

  implicit val actorSystem: ActorSystem = _actorSystem
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer = makeMat()
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val node = Cluster(actorSystem)

  val replicator: ActorRef = DistributedData(actorSystem).replicator

  def makeMat(): Materializer = ActorMaterializer()
  def terminate(): Future[Terminated] = actorSystem.terminate()
}
