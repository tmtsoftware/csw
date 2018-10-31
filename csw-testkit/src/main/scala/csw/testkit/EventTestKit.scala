package csw.testkit

import java.util.Optional

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.event.client.internal.commons.EventServiceConnection
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.RegistrationResult
import csw.network.utils.SocketUtils.getFreePort
import csw.testkit.redis.RedisStore

import scala.compat.java8.OptionConverters.RichOptionalGeneric

final class EventTestKit private (config: Config, settings: Option[TestKitSettings]) extends RedisStore {

  override implicit val system: ActorSystem        = ActorSystem("event-test-kit", config)
  override implicit val timeout: Timeout           = testKitSettings.DefaultTimeout
  override protected val masterId: String          = config.getString("csw-event.redis.masterId")
  override protected val connection: TcpConnection = EventServiceConnection.value

  implicit def testKitSettings: TestKitSettings = settings.getOrElse(TestKitSettings(config))

  /**
   * Scala API to Start Event service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's event service with location service
   */
  def startEventService(sentinelPort: Int = getFreePort, serverPort: Int = getFreePort): RegistrationResult =
    start(sentinelPort, serverPort)

  /**
   * Java API to Start Event service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's event service with location service
   */
  def startEventService(sentinelPort: Optional[Int], serverPort: Optional[Int]): Unit =
    startEventService(sentinelPort.orElse(getFreePort), serverPort.orElse(getFreePort))

  /**
   * Shutdown Event service
   *
   * When the test has completed, make sure you shutdown event service.
   * This will terminate actor system and stop redis sentinel and redis server.
   */
  def shutdownEventService(): Unit = shutdown()

}

object EventTestKit {

  /**
   * Create a EventTestKit
   *
   * When the test has completed you should shutdown the event service
   * with [[EventTestKit#shutdownEventService]].
   *
   * @return handle to EventTestKit which can be used to start and stop event service
   */
  def apply(): EventTestKit = new EventTestKit(ConfigFactory.load(), None)

  /**
   * Create a EventTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to EventTestKit which can be used to start and stop event service
   */
  def apply(testKitSettings: TestKitSettings): EventTestKit = new EventTestKit(ConfigFactory.load(), Some(testKitSettings))

  /**
   * Scala API for creating EventTestKit
   *
   * @param config custom configuration with which to start event service
   * @param testKitSettings custom testKitSettings
   * @return handle to EventTestKit which can be used to start and stop event service
   */
  def apply(config: Config, testKitSettings: Option[TestKitSettings]): EventTestKit =
    new EventTestKit(config, testKitSettings)

  /**
   * Java API for creating EventTestKit
   *
   * @param config custom configuration with which to start event service
   * @param testKitSettings custom testKitSettings
   * @return handle to EventTestKit which can be used to start and stop event service
   */
  def create(config: Config, testKitSettings: Optional[TestKitSettings]): EventTestKit =
    apply(config, testKitSettings.asScala)

}
