package csw.framework
import akka.actor
import akka.actor.Terminated
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.UntypedActorSystemOps
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import csw.commons.redis.EmbeddedRedis
import csw.commons.utils.SocketUtils
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{RegistrationResult, TcpRegistration}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import redis.embedded.{RedisSentinel, RedisServer}

class FrameworkTestWiring(val seedPort: Int = SocketUtils.getFreePort) extends EmbeddedRedis {

  implicit val seedActorSystem: actor.ActorSystem = ActorSystemFactory.remote()
  implicit val typedSystem: ActorSystem[_]        = seedActorSystem.toTyped
  implicit val mat: Materializer                  = ActorMaterializer()
  val seedLocationService: LocationService        = HttpLocationServiceFactory.makeLocalClient

  val testActorSystem: actor.ActorSystem = ActorSystemFactory.remote()

  def startSentinelAndRegisterService(
      connection: TcpConnection,
      masterId: String
  ): (RegistrationResult, RedisSentinel, RedisServer) =
    withSentinel(masterId = masterId) { (sentinelPort, _) ⇒
      seedLocationService
        .register(TcpRegistration(connection, sentinelPort))
        .await
    }

  def shutdown(): Terminated = {
    Http(seedActorSystem).shutdownAllConnectionPools().await
    testActorSystem.terminate().await
    seedActorSystem.terminate().await
  }
}
