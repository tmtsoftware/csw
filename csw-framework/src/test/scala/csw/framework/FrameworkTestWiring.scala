package csw.framework
import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import csw.commons.redis.EmbeddedRedis
import csw.event.client.helpers.TestFutureExt.RichFuture
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.TcpConnection
import csw.location.models.TcpRegistration
import csw.network.utils.SocketUtils
import redis.embedded.{RedisSentinel, RedisServer}

class FrameworkTestWiring(val seedPort: Int = SocketUtils.getFreePort) extends EmbeddedRedis {

  implicit val seedActorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "seed-system")
  implicit val mat: Materializer                                   = Materializer(seedActorSystem)
  val seedLocationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient

  def startSentinelAndRegisterService(
      connection: TcpConnection,
      masterId: String
  ): (RegistrationResult, RedisSentinel, RedisServer) =
    withSentinel(masterId = masterId) { (sentinelPort, _) =>
      seedLocationService
        .register(TcpRegistration(connection, sentinelPort))
        .await
    }

  def shutdown(): Done = {
    seedActorSystem.terminate()
    seedActorSystem.whenTerminated.await
  }
}
