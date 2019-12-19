package csw.testkit

import java.util.Optional

import akka.actor.typed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.location.api.scaladsl.RegistrationResult
import csw.location.models.Connection.TcpConnection
import csw.network.utils.SocketUtils.getFreePort
import csw.testkit.redis.RedisStore

/**
 * AlarmTestKit supports starting Alarm server using embedded redis internally (sentinel + master)
 * and registering it with location service
 *
 * Example:
 * {{{
 *   private val testKit = AlarmTestKit()
 *
 *   // starting alarm server (start sentinel and master on default ports specified in configuration file)
 *   // it will also register AlarmService with location service
 *   testKit.startAlarmService()
 *
 *   // stopping alarm server
 *   testKit.shutdownAlarmService()
 *
 * }}}
 *
 */
final class AlarmTestKit private (_system: ActorSystem[SpawnProtocol.Command], testKitSettings: TestKitSettings)
    extends RedisStore {

  override implicit val system: ActorSystem[SpawnProtocol.Command] = _system
  override implicit lazy val timeout: Timeout                      = testKitSettings.DefaultTimeout
  override protected lazy val masterId: String                     = system.settings.config.getString("csw-alarm.redis.masterId")
  override protected lazy val connection: TcpConnection            = AlarmServiceConnection.value

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
  def apply(
      actorSystem: ActorSystem[SpawnProtocol.Command] = typed.ActorSystem(SpawnProtocol(), "alarm-testkit"),
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
  ): AlarmTestKit = new AlarmTestKit(actorSystem, testKitSettings)

  /**
   * Java API to create a EventTestKit
   *
   * @return handle to EventTestKit which can be used to start and stop event service
   */
  def create(actorSystem: ActorSystem[SpawnProtocol.Command]): AlarmTestKit = apply(actorSystem)

  /**
   * Java API to create a AlarmTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to AlarmTestKit which can be used to start and stop alarm service
   */
  def create(testKitSettings: TestKitSettings): AlarmTestKit = apply(testKitSettings = testKitSettings)

}
