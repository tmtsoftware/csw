package csw.time.clock.natives.models

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

private[time] sealed trait TMTClock {
  def utcInstant: Instant
  def taiInstant: Instant
  def offset: Int
  def setTaiOffset(offset: Int): Unit
}

private[time] object TMTClock {
  val clock: TMTClock = new NonLinuxClock()
}

private[time] class NonLinuxClock extends TMTClock {
  private val internal_offset: AtomicInteger = new AtomicInteger(0)

  override def offset: Int         = internal_offset.get()
  override def utcInstant: Instant = Instant.now()
  override def taiInstant: Instant = Instant.now().plusSeconds(offset)
  // This api is only for testing purpose and might not set offset value in one attempt in concurrent environment
  override def setTaiOffset(_offset: Int): Unit = internal_offset.compareAndSet(offset, _offset)
}
