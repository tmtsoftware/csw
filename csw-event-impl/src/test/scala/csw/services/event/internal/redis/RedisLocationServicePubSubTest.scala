package csw.services.event.internal.redis

import akka.actor.ActorSystem
import com.github.sebruck.EmbeddedRedis
import csw.services.event.RedisFactory
import csw.services.event.helpers.RegistrationFactory
import csw.services.event.helpers.TestFutureExt.RichFuture
import csw.services.event.internal.EventServicePubSubTestFramework
import csw.services.event.internal.commons.EventServiceConnection
import csw.services.event.scaladsl.{EventPublisher, EventSubscriber}
import csw.services.location.commons.ClusterAwareSettings
import csw.services.location.models.TcpRegistration
import csw.services.location.scaladsl.LocationServiceFactory
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}
import redis.embedded.RedisServer

class RedisLocationServicePubSubTest extends FunSuite with Matchers with BeforeAndAfterAll with EmbeddedRedis {
  private val seedPort        = 3558
  private val redisPort       = 6379
  private val clusterSettings = ClusterAwareSettings.joinLocal(seedPort)
  private val redis           = RedisServer.builder().setting(s"bind ${clusterSettings.hostname}").port(redisPort).build()
  redis.start()

  private implicit val actorSystem: ActorSystem = clusterSettings.system
  private val locationService                   = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(seedPort))
  private val redisFactory                      = new RedisFactory(locationService, actorSystem)
  private val tcpRegistration: TcpRegistration  = RegistrationFactory.tcp(EventServiceConnection.value, redisPort)
  locationService.register(tcpRegistration).await
  private val publisher  = redisFactory.publisher().await
  private val subscriber = redisFactory.subscriber().await
  private val framework  = new EventServicePubSubTestFramework(publisher, subscriber)

  override def afterAll(): Unit = {
    publisher.shutdown()
    redis.stop()
    actorSystem.terminate().await
  }

  test("Pub Sub") {
    framework.pubSub()
  }

}
