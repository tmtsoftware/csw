package csw.time.client

import java.time.Instant

import csw.time.client.internal.{LinuxClock, NonLinuxClock, OSType, TMTClock}

object TMTClock {
  private lazy val defaultInstance = instance()

  def now(): Instant = defaultInstance.now()

  private[time] def instance(offset: Int = 0): TMTClock = OSType.value match {
    case OSType.Linux => new LinuxClock()
    case OSType.Other => new NonLinuxClock(offset)
  }
}
