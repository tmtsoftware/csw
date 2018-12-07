package csw.time.api.scaladsl
import csw.time.api.models.Cancellable
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}

trait TimeService {

  def utcTime(): UtcInstant

  def taiTime(): TaiInstant

  def taiOffset(): Int

  def toTai(utcInstant: UtcInstant): TaiInstant

  def toUtc(taiInstant: TaiInstant): UtcInstant

  def scheduleOnce(startTime: TaiInstant)(task: Runnable): Cancellable

  private[time] def setTaiOffset(offset: Int): Unit
}
