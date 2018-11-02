package csw.testkit

import java.util.Optional

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.RegistrationResult
import csw.network.utils.SocketUtils.getFreePort
import csw.testkit.redis.RedisStore

final class AlarmTestKit private (testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())) extends RedisStore {

  override implicit lazy val system: ActorSystem        = ActorSystem("alarm-test-kit")
  override implicit lazy val timeout: Timeout           = testKitSettings.DefaultTimeout
  override protected lazy val masterId: String          = system.settings.config.getString("csw-alarm.redis.masterId")
  override protected lazy val connection: TcpConnection = AlarmServiceConnection.value

  private def getSentinelPort: Int = testKitSettings.AlarmSentinelPort.getOrElse(getFreePort)
  private def getMasterPort: Int   = testKitSettings.AlarmMasterPort.getOrElse(getFreePort)

  /**
   * Scala API to Start Alarm service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's Alarm service with location service
   */
  def startAlarmService(sentinelPort: Int = getSentinelPort, serverPort: Int = getMasterPort): RegistrationResult =
    start(sentinelPort, serverPort)

  /**
   * Java API to Start Alarm service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's Alarm service with location service
   */
  def startAlarmService(sentinelPort: Optional[Int], serverPort: Optional[Int]): Unit =
    startAlarmService(sentinelPort.orElse(getSentinelPort), serverPort.orElse(getMasterPort))

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
  def apply(): AlarmTestKit = new AlarmTestKit()

  /**
   * Create a AlarmTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to AlarmTestKit which can be used to start and stop alarm service
   */
  def apply(testKitSettings: TestKitSettings): AlarmTestKit = new AlarmTestKit(testKitSettings)

}
