package csw.services.event.internal.redis

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.event.exceptions.PublishFailed
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.helpers.{RegistrationFactory, Utils}
import csw.services.event.internal.commons.{EventServiceConnection, Wiring}
import csw.services.event.scaladsl.RedisFactory
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.scaladsl.LoggingSystemFactory
import io.lettuce.core.ClientOptions.DisconnectedBehavior
import io.lettuce.core.{ClientOptions, RedisClient}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

class FailureTest extends FunSuite with Matchers with MockitoSugar with BeforeAndAfterAll {

  private val seedPort        = 3560
  private val redisPort       = 6379
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val tcpRegistration = RegistrationFactory.tcp(EventServiceConnection.value, redisPort)
  locationService.register(tcpRegistration).await

  private val redis = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()

  private implicit val actorSystem: ActorSystem = clusterSettings.system

  //TODO: Logging is kept on to debug the flaky test on jenkins
  LoggingSystemFactory.start("", "", "", actorSystem)

  private val redisClient = RedisClient.create()
  redisClient.setOptions(
    ClientOptions.builder().autoReconnect(false).disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS).build()
  )

  private val wiring       = new Wiring(actorSystem)
  private val redisFactory = new RedisFactory(redisClient, locationService, wiring)
  private val publisher    = redisFactory.publisher().await

  override def beforeAll(): Unit = {
    redis.start()
  }

  override def afterAll(): Unit = {
    redisClient.shutdown(0, 10, TimeUnit.SECONDS)
    redis.stop()
    wiring.shutdown(TestFinishedReason).await
  }

  test("Redis - failure in publishing should fail future with PublishFailed exception") {

    publisher.publish(Utils.makeEvent(1)).await

    publisher.shutdown().await

    intercept[PublishFailed] {
      publisher.publish(Utils.makeEvent(2)).await
    }
  }
}
