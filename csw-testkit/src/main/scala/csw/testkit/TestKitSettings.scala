package csw.testkit

import akka.actor.ActorSystem
import akka.util.Timeout
import com.typesafe.config.Config

import scala.compat.java8.DurationConverters.DurationOps

object TestKitSettings {

  /**
   * Reads configuration settings from `csw.testkit` section.
   */
  def apply(system: ActorSystem): TestKitSettings = apply(system.settings.config.getConfig("csw.testkit"))

  /**
   * Reads configuration settings from given `Config` that
   * must have the same layout as the `csw.testkit` section.
   */
  def apply(config: Config): TestKitSettings = new TestKitSettings(config)

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

}
