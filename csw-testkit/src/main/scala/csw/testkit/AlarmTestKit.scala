package csw.testkit

import java.util.Optional

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.RegistrationResult
import csw.network.utils.SocketUtils.getFreePort
import csw.testkit.redis.RedisStore

import scala.compat.java8.OptionConverters.RichOptionalGeneric

final class AlarmTestKit private (config: Config, settings: Option[TestKitSettings]) extends RedisStore {

  override implicit lazy val system: ActorSystem        = ActorSystem("alarm-test-kit", config)
  override implicit lazy val timeout: Timeout           = testKitSettings.DefaultTimeout
  override protected lazy val masterId: String          = config.getString("csw-alarm.redis.masterId")
  override protected lazy val connection: TcpConnection = AlarmServiceConnection.value

  lazy val testKitSettings: TestKitSettings = settings.getOrElse(TestKitSettings(config))

  /**
   * Scala API to Start Alarm service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's Alarm service with location service
   */
  def startAlarmService(sentinelPort: Int = getFreePort, serverPort: Int = getFreePort): RegistrationResult =
    start(sentinelPort, serverPort)

  /**
   * Java API to Start Alarm service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's Alarm service with location service
   */
  def startAlarmService(sentinelPort: Optional[Int], serverPort: Optional[Int]): Unit =
    startAlarmService(sentinelPort.orElse(getFreePort), serverPort.orElse(getFreePort))

  /**
   * Shutdown Alarm service
   *
   * When the test has completed, make sure you shutdown Alarm service.
   * This will terminate actor system and stop redis sentinel and redis server.
   */
  def shutdownAlarmService(): Unit = shutdown()

}

object AlarmTestKit {

  /**
   * Create a AlarmTestKit
   *
   * When the test has completed you should shutdown the alarm service
   * with [[AlarmTestKit#shutdownAlarmService]].
   *
   * @return handle to AlarmTestKit which can be used to start and stop alarm service
   */
  def apply(): AlarmTestKit = new AlarmTestKit(ConfigFactory.load(), None)

  /**
   * Create a AlarmTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to AlarmTestKit which can be used to start and stop alarm service
   */
  def apply(testKitSettings: TestKitSettings): AlarmTestKit = new AlarmTestKit(ConfigFactory.load(), Some(testKitSettings))

  /**
   * Scala API for creating AlarmTestKit
   *
   * @param config custom configuration with which to start alarm service
   * @param testKitSettings custom testKitSettings
   * @return handle to AlarmTestKit which can be used to start and stop alarm service
   */
  def apply(config: Config, testKitSettings: Option[TestKitSettings]): AlarmTestKit =
    new AlarmTestKit(config, testKitSettings)

  /**
   * Java API for creating AlarmTestKit
   *
   * @param config custom configuration with which to start alarm service
   * @param testKitSettings custom testKitSettings
   * @return handle to AlarmTestKit which can be used to start and stop alarm service
   */
  def create(config: Config, testKitSettings: Optional[TestKitSettings]): AlarmTestKit =
    apply(config, testKitSettings.asScala)

}
