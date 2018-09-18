package csw.framework
import akka.actor
import akka.actor.Terminated
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.stream.{ActorMaterializer, Materializer}
import csw.commons.redis.EmbeddedRedis
import csw.commons.utils.SocketUtils
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.TcpRegistration
import csw.location.api.scaladsl.LocationService
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.location.api.commons.ClusterSettings
import csw.location.api.models.RegistrationResult
import csw.location.scaladsl.LocationServiceFactory
import csw.logging.commons.LogAdminActorFactory
import redis.embedded.{RedisSentinel, RedisServer}

class FrameworkTestWiring(val seedPort: Int = SocketUtils.getFreePort) extends EmbeddedRedis {

  implicit val seedActorSystem: actor.ActorSystem = ClusterSettings().onPort(seedPort).system
  implicit val typedSystem: ActorSystem[_]        = seedActorSystem.toTyped
  implicit val mat: Materializer                  = ActorMaterializer()
  val seedLocationService: LocationService        = LocationServiceFactory.withSystem(seedActorSystem)

  val testActorSystem: actor.ActorSystem = ClusterSettings().joinLocal(seedPort).system

  def startSentinelAndRegisterService(
      connection: TcpConnection,
      masterId: String
  ): (RegistrationResult, RedisSentinel, RedisServer) =
    withSentinel(masterId = masterId) { (sentinelPort, _) ⇒
      seedLocationService
        .register(TcpRegistration(connection, sentinelPort, LogAdminActorFactory.make(seedActorSystem)))
        .await
    }

  def shutdown(): Terminated = {
    testActorSystem.terminate().await
    seedActorSystem.terminate().await
  }
}
