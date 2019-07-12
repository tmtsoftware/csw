package csw.framework
import akka.Done
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.commons.redis.EmbeddedRedis
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.TcpConnection
import csw.location.model.scaladsl.TcpRegistration
import csw.network.utils.SocketUtils
import redis.embedded.{RedisSentinel, RedisServer}

class FrameworkTestWiring(val seedPort: Int = SocketUtils.getFreePort) extends EmbeddedRedis {

  implicit val seedActorSystem: ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior, "seed-system")
  implicit val mat: Materializer                           = ActorMaterializer()
  val seedLocationService: LocationService                 = HttpLocationServiceFactory.makeLocalClient

  def startSentinelAndRegisterService(
      connection: TcpConnection,
      masterId: String
  ): (RegistrationResult, RedisSentinel, RedisServer) =
    withSentinel(masterId = masterId) { (sentinelPort, _) ⇒
      seedLocationService
        .register(TcpRegistration(connection, sentinelPort))
        .await
    }

  def shutdown(): Done = {
    Http(seedActorSystem.toUntyped).shutdownAllConnectionPools().await
    seedActorSystem.terminate()
    seedActorSystem.whenTerminated.await
  }
}
