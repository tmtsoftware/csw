package csw.services.location.scaladsl

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.Cluster
import akka.cluster.ddata.DistributedData
import akka.stream.{ActorMaterializer, Materializer}
import csw.services.location.internal.{Settings, Terminator}

import scala.concurrent.{ExecutionContext, Future}

class ActorRuntime(_actorSystem: ActorSystem) {
  def this(settings: Settings) = this(ActorSystem(settings.name, settings.config))
  def this() = this(Settings())

  val hostname: String = _actorSystem.settings.config.getString("akka.remote.netty.tcp.hostname")

  implicit val actorSystem: ActorSystem = _actorSystem
  implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit val mat: Materializer = makeMat()
  implicit val cluster = Cluster(actorSystem)

  val replicator: ActorRef = DistributedData(actorSystem).replicator

  def initialize(): Unit = {
    val emptySeeds = actorSystem.settings.config.getStringList("akka.cluster.seed-nodes").isEmpty
    if(emptySeeds) {
      cluster.join(cluster.selfAddress)
    }
  }

  def makeMat(): Materializer = ActorMaterializer()

  def terminate(): Future[Done] = Terminator.terminate(actorSystem)

  initialize()
}
