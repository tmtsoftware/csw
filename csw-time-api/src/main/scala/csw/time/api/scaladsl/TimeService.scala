package csw.time.api.scaladsl
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}

trait TimeService {

  def utcTime(): UtcInstant

  def taiTime(): TaiInstant

  def taiOffset(): Int

}
