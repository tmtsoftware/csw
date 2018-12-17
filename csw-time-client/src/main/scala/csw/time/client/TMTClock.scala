package csw.time.client

import java.time.Instant

import csw.time.api.models.TMTTime.{TAITime, UTCTime}
import csw.time.client.internal.{LinuxClock, NonLinuxClock, OSType, TMTClock}

object TMTClock {
  private lazy val defaultInstance = instance()

  // todo: can be removed if there is no usage for now
  def now(): Instant    = defaultInstance.now() // this instant is extracted from utc time and does not include tai offset
  def utcNow(): UTCTime = defaultInstance.utcTime()
  def taiNow(): TAITime = defaultInstance.taiTime()

  private[time] def instance(offset: Int = 0): TMTClock = OSType.value match {
    case OSType.Linux => new LinuxClock()
    case OSType.Other => new NonLinuxClock(offset)
  }
}
