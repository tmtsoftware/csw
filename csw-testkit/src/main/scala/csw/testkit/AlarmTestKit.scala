/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.testkit

import akka.Done
import akka.actor.typed
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import csw.alarm.api.scaladsl.AlarmAdminService
import csw.alarm.client.AlarmServiceFactory
import csw.alarm.client.internal.commons.AlarmServiceConnection
import csw.alarm.models.FullAlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.network.utils.SocketUtils.getFreePort
import csw.testkit.redis.RedisStore
import io.lettuce.core.RedisClient

import java.util.Optional
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration

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
 */
final class AlarmTestKit private (
    redisClient: RedisClient,
    _system: ActorSystem[SpawnProtocol.Command],
    testKitSettings: TestKitSettings
) extends RedisStore {

  override implicit val system: ActorSystem[SpawnProtocol.Command] = _system
  override implicit lazy val timeout: Timeout                      = testKitSettings.DefaultTimeout
  override protected lazy val masterId: String                     = system.settings.config.getString("csw-alarm.redis.masterId")
  override protected lazy val connection: TcpConnection            = AlarmServiceConnection.value
  lazy val locationService: LocationService                        = HttpLocationServiceFactory.makeLocalClient(system)

  private val alarmServiceFactory: AlarmServiceFactory = new AlarmServiceFactory(redisClient)
  private lazy val alarmService: AlarmAdminService     = alarmServiceFactory.makeAdminApi(locationService)

  private def getSentinelPort: Int = testKitSettings.AlarmSentinelPort.getOrElse(getFreePort)

  private def getMasterPort: Int = testKitSettings.AlarmMasterPort.getOrElse(getFreePort)

  /**
   * Scala API to Start Alarm service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's Alarm service with location service
   */
  def startAlarmService(sentinelPort: Int = getSentinelPort, serverPort: Int = getMasterPort): RegistrationResult =
    start(sentinelPort, serverPort, keyspaceEvent = true)

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

  /**
   * Loads data for all alarms in alarm store i.e. metadata of alarms for e.g. subsystem, component, name, etc. and status of
   * alarms for e.g. acknowledgement status, latch status, etc.
   *
   * @note severity of the alarm is not loaded in store and is by default inferred as Disconnected until component starts
   *       updating severity
   * @see [[csw.alarm.models.AlarmMetadata]],
   *     [[csw.alarm.models.AlarmStatus]]
   * @param config represents the data for all alarms to be loaded in alarm store
   * @param reset the alarm store before loading the data
   * @return Done when data is loaded successfully in alarm store or fails with
   *         [[csw.alarm.api.exceptions.ConfigParseException]]
   */
  def initAlarms(config: Config, reset: Boolean = false): Done =
    Await.result(alarmService.initAlarms(config, reset), new FiniteDuration(8, TimeUnit.SECONDS))

  /**
   * Fetches the severity of the given alarm from the alarm store
   *
   * @param alarmKey represents a unique alarm in alarm store
   * @return the alarm severity or fails with [[csw.alarm.api.exceptions.KeyNotFoundException]]
   */
  def getCurrentSeverity(alarmKey: AlarmKey): FullAlarmSeverity =
    Await.result(alarmService.getCurrentSeverity(alarmKey), new FiniteDuration(1, TimeUnit.SECONDS))
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
      redisClient: RedisClient = RedisClient.create(),
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
  ): AlarmTestKit = new AlarmTestKit(redisClient, actorSystem, testKitSettings)

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

  /**
   * Java API to create a EventTestKit
   *
   * @return handle to EventTestKit which can be used to start and stop event service
   */
  def create(redisClient: RedisClient): AlarmTestKit = apply(redisClient = redisClient)

}
