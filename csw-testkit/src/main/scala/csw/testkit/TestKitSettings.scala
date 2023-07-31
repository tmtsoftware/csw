/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.testkit

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.util.Timeout
import com.typesafe.config.Config

import scala.jdk.DurationConverters.*

object TestKitSettings {

  /**
   * Reads configuration settings from `csw.testkit` section.
   */
  def apply(system: ActorSystem[_]): TestKitSettings = apply(system.settings.config)

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `csw.testkit` section.
   */
  def apply(config: Config): TestKitSettings = new TestKitSettings(config.getConfig("csw.testkit"))

  /**
   * Java API: Reads configuration settings from ``csw.testkit`` section.
   */
  def create(system: ActorSystem[_]): TestKitSettings = apply(system)

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `pekko.actor.testkit.typed` section.
   */
  def create(config: Config): TestKitSettings = apply(config)
}

final class TestKitSettings(val config: Config) {

  /** `DefaultTimeout` used for awaiting on future values */
  val DefaultTimeout: Timeout = Timeout(config.getDuration("default-timeout").toScala)

  val LocationClusterPort: Int       = locationConfig.getInt("cluster-port")
  val LocationHttpPort: Int          = locationConfig.getInt("http-port")
  val LocationAuthClusterPort: Int   = locationAuthConfig.getInt("cluster-port")
  val LocationAuthHttpPort: Int      = locationAuthConfig.getInt("http-port")
  val ConfigPort: Option[Int]        = toOption(configServerConfig.getInt("port"))
  val EventSentinelPort: Option[Int] = toOption(eventConfig.getInt("sentinel-port"))
  val EventMasterPort: Option[Int]   = toOption(eventConfig.getInt("master-port"))
  val AlarmSentinelPort: Option[Int] = toOption(alarmConfig.getInt("sentinel-port"))
  val AlarmMasterPort: Option[Int]   = toOption(alarmConfig.getInt("master-port"))

  private def locationConfig     = config.getConfig("location")
  private def locationAuthConfig = config.getConfig("location-with-auth")
  private def configServerConfig = config.getConfig("config")
  private def eventConfig        = config.getConfig("event")
  private def alarmConfig        = config.getConfig("alarm")

  private def toOption(i: Int) =
    if (i == 0) None
    else Some(i)

}
