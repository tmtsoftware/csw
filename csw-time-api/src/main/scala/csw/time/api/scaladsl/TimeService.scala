package csw.time.api.scaladsl
import csw.time.api.models.CswInstant.{TaiInstant, UtcInstant}

trait TimeService {

  def UtcTime(): UtcInstant

  def TaiTime(): TaiInstant

  def TaiOffset(): Int
}
