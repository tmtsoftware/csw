package csw.time.clock.natives.models

import java.time.Instant

private[time] sealed trait TMTClock {
  def utcInstant: Instant
  def taiInstant: Instant
  def offset: Int
}

private[time] object TMTClock {
  val clock: TMTClock = new NonLinuxClock()
}

private[time] class NonLinuxClock extends TMTClock {
  override def offset: Int         = TimeConstants.taiOffset
  override def utcInstant: Instant = Instant.now()
  override def taiInstant: Instant = Instant.now().plusSeconds(offset)
}
