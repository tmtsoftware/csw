/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.testkit

import java.util.Optional

import org.apache.pekko.actor.typed
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.event.client.internal.commons.EventServiceConnection
import csw.location.api.scaladsl.RegistrationResult
import csw.location.api.models.Connection.TcpConnection
import csw.network.utils.SocketUtils.getFreePort
import csw.testkit.redis.RedisStore

/**
 * EventTestKit supports starting Alarm server using embedded redis internally (sentinel + master)
 * and registering it with location service
 *
 * Example:
 * {{{
 *   private val testKit = EventTestKit()
 *
 *   // starting event server (start sentinel on port 6379 and master on port 6378)
 *   // it will also register EventService with location service
 *   testKit.startEventService(sentinelPort = 6379, serverPort = 6378)
 *
 *   // stopping event server
 *   testKit.shutdownEventService()
 *
 * }}}
 */
final class EventTestKit private (_system: typed.ActorSystem[?], testKitSettings: TestKitSettings) extends RedisStore {

  override implicit val system: typed.ActorSystem[?]    = _system
  override implicit lazy val timeout: Timeout           = testKitSettings.DefaultTimeout
  override protected lazy val masterId: String          = system.settings.config.getString("csw-event.redis.masterId")
  override protected lazy val connection: TcpConnection = EventServiceConnection.value

  private def getSentinelPort: Int = testKitSettings.EventSentinelPort.getOrElse(getFreePort)
  private def getMasterPort: Int   = testKitSettings.EventMasterPort.getOrElse(getFreePort)

  /**
   * Scala API to Start Event service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's event service with location service
   */
  def startEventService(sentinelPort: Int = getSentinelPort, serverPort: Int = getMasterPort): RegistrationResult =
    start(sentinelPort, serverPort, keyspaceEvent = false)

  /**
   * Java API to Start Event service
   *
   * It will start redis sentinel and redis server on provided ports
   * and then register's event service with location service
   */
  def startEventService(sentinelPort: Optional[Int], serverPort: Optional[Int]): Unit =
    startEventService(sentinelPort.orElse(getSentinelPort), serverPort.orElse(getMasterPort))

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
  def apply(
      actorSystem: typed.ActorSystem[?] = typed.ActorSystem(Behaviors.empty, "alarm-testkit"),
      testKitSettings: TestKitSettings = TestKitSettings(ConfigFactory.load())
  ): EventTestKit = new EventTestKit(actorSystem, testKitSettings)

  /**
   * Java API to create a EventTestKit
   *
   * @param actorSystem
   * @return handle to EventTestKit which can be used to start and stop event service
   */
  def create(actorSystem: typed.ActorSystem[?]): EventTestKit = apply(actorSystem)

  /**
   * Java API to create a EventTestKit
   *
   * @param testKitSettings custom testKitSettings
   * @return handle to EventTestKit which can be used to start and stop event service
   */
  def create(testKitSettings: TestKitSettings): EventTestKit = apply(testKitSettings = testKitSettings)

}
