package csw.time.client

import csw.time.client.internal.OSType

import scala.concurrent.duration.DurationLong

trait TestProperties {
  def precision: Int
  def allowedJitterInNanos: Int
}

object TestProperties {
  class LinuxProperties extends TestProperties {
    override val precision: Int            = 9
    override val allowedJitterInNanos: Int = 5.millis.toNanos.toInt
  }

  class NonLinuxProperties extends TestProperties {
    override val precision: Int            = 3
    override val allowedJitterInNanos: Int = 7.millis.toNanos.toInt
  }

  val instance: TestProperties = OSType.value match {
    case OSType.Linux => new LinuxProperties
    case OSType.Other => new NonLinuxProperties
  }

}
