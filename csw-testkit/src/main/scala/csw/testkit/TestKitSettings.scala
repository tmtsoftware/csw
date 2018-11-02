package csw.testkit

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.Config

import scala.compat.java8.DurationConverters.DurationOps

object TestKitSettings {

  /**
   * Reads configuration settings from `csw.testkit` section.
   */
  def apply(system: ActorSystem): TestKitSettings = apply(system.settings.config)

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `csw.testkit` section.
   */
  def apply(config: Config): TestKitSettings = new TestKitSettings(config.getConfig("csw.testkit"))

  /**
   * Java API: Reads configuration settings from ``csw.testkit`` section.
   */
  def create(system: ActorSystem): TestKitSettings = apply(system)

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `akka.actor.testkit.typed` section.
   */
  def create(config: Config): TestKitSettings = apply(config)
}

final class TestKitSettings(val config: Config) {

  /** `DefaultTimeout` used for awaiting on future values */
  val DefaultTimeout: Timeout = Timeout(config.getDuration("default-timeout").toScala)

  val LocationClusterPort: Option[Int] = toOption(locationConfig.getInt("cluster-port"))
  val ConfigPort: Option[Int]          = toOption(configServerConfig.getInt("port"))
  val EventSentinelPort: Option[Int]   = toOption(eventConfig.getInt("sentinel-port"))
  val EventMasterPort: Option[Int]     = toOption(eventConfig.getInt("master-port"))
  val AlarmSentinelPort: Option[Int]   = toOption(alarmConfig.getInt("sentinel-port"))
  val AlarmMasterPort: Option[Int]     = toOption(alarmConfig.getInt("master-port"))

  private def locationConfig     = config.getConfig("location")
  private def configServerConfig = config.getConfig("config")
  private def eventConfig        = config.getConfig("event")
  private def alarmConfig        = config.getConfig("alarm")

  private def toOption(i: Int) =
    if (i == 0) None
    else Some(i)

}
