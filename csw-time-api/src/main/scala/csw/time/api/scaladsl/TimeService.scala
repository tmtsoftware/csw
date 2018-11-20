package csw.time.api.scaladsl
import csw.time.api.models.CswInstant

trait TimeService {

  def UTCTime(): CswInstant

  def TAITime(): CswInstant

  def TAIOffset(): Int
}
