package csw.time.api

trait TimeService {

  def UTCTime(): CswInstant

  def TAITime(): CswInstant

  def TAIOffset(): Int
}
