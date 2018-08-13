package csw.services.alarm.client
import akka.actor.ActorSystem
import csw.commons.utils.SocketUtils.getFreePort
import csw.messages.commons.CoordinatedShutdownReasons.TestFinishedReason
import csw.services.alarm.client.internal.commons.AlarmServiceConnection
import csw.services.alarm.client.internal.helpers.TestFutureExt.RichFuture
import csw.services.location.commons.{ActorSystemFactory, ClusterAwareSettings}
import csw.services.location.models.TcpRegistration
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.logging.commons.LogAdminActorFactory
import io.lettuce.core.RedisClient
import org.scalatest.{FunSuite, Matchers}

// DEOPSCSW-481: Component Developer API available to all CSW components
class AlarmServiceFactoryTest extends FunSuite with Matchers {

  implicit val actorSystem: ActorSystem = ActorSystemFactory.remote()

  private val redisClient = RedisClient.create()
  val alarmServiceFactory = new AlarmServiceFactory(redisClient)

  private val seedSystem      = ClusterAwareSettings.onPort(getFreePort).system
  private val locationService = LocationServiceFactory.withSystem(seedSystem)

  locationService
    .register(TcpRegistration(AlarmServiceConnection.value, getFreePort, LogAdminActorFactory.make(seedSystem)))
    .await

  test("makeAdminApi should not throw exception") {
    noException shouldBe thrownBy {
      alarmServiceFactory.makeAdminApi(locationService).await
    }
  }

  test("makeClientApi should not throw exception") {
    noException shouldBe thrownBy {
      alarmServiceFactory.makeClientApi(locationService).await
    }
  }

  protected def afterAll(): Unit = {
    locationService.shutdown(TestFinishedReason).await
    redisClient.shutdown()
    actorSystem.terminate().await
  }
}
