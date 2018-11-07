package csw.testkit.redis
import java.util.Optional

import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{RegistrationResult, TcpRegistration}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.SocketUtils.getFreePort
import csw.testkit.internal.TestKitUtils
import redis.embedded.{RedisSentinel, RedisServer}

import scala.concurrent.ExecutionContext

private[testkit] trait RedisStore extends EmbeddedRedis {

  implicit def system: ActorSystem
  implicit def timeout: Timeout
  protected def masterId: String
  protected def connection: TcpConnection

  implicit lazy val mat: Materializer    = ActorMaterializer()
  implicit lazy val ec: ExecutionContext = system.dispatcher

  var redisSentinel: Option[RedisSentinel] = None
  var redisServer: Option[RedisServer]     = None

  implicit lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  def start(sentinelPort: Int = getFreePort, serverPort: Int = getFreePort): RegistrationResult = {
    val tuple = startSentinel(sentinelPort, serverPort, masterId)
    redisSentinel = Some(tuple._1)
    redisServer = Some(tuple._2)
    val resultF = locationService.register(TcpRegistration(connection, sentinelPort))
    TestKitUtils.await(resultF, timeout)
  }

  def start(sentinelPort: Optional[Int], serverPort: Optional[Int]): Unit =
    start(sentinelPort.orElse(getFreePort), serverPort.orElse(getFreePort))

  def shutdown(): Unit = {
    redisServer.foreach(_.stop())
    redisSentinel.foreach(_.stop())
    TestKitUtils.await(Http().shutdownAllConnectionPools(), timeout)
    TestKitUtils.coordShutdown(CoordinatedShutdown(system).run, timeout)
  }
}
