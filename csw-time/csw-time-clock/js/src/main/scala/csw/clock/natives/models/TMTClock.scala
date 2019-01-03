package csw.clock.natives.models

import java.time.Instant

sealed trait TMTClock {
  def utcInstant: Instant
  def taiInstant: Instant
  def offset: Int
  def setTaiOffset(offset: Int): Unit
}
object TMTClock {
  val clock: TMTClock = new DummyClock()
}

class DummyClock extends TMTClock {

  override def offset: Int                      = 0
  override def utcInstant: Instant              = Instant.now()
  override def taiInstant: Instant              = Instant.now().plusSeconds(offset)
  override def setTaiOffset(_offset: Int): Unit = ()
}
