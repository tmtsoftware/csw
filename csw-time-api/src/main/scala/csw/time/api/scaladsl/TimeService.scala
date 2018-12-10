package csw.time.api.scaladsl

import java.time.{Duration, ZoneId, ZoneOffset}

import csw.time.api.models.Cancellable
import csw.time.api.models.TMTTime.{TAITime, UTCTime}

trait TimeService {
  private val localZoneId: ZoneId  = ZoneId.systemDefault()
  private val hawaiiZoneId: ZoneId = ZoneId.of("US/Hawaii")

  def utcTime(): UTCTime         = utcTimeAt(ZoneOffset.UTC)
  def utcTimeAtLocal(): UTCTime  = utcTimeAt(localZoneId)
  def utcTimeAtHawaii(): UTCTime = utcTimeAt(hawaiiZoneId)
  def utcTimeAt(zoneId: ZoneId): UTCTime

  def taiTime(): TAITime         = taiTimeAt(ZoneOffset.UTC)
  def taiTimeAtLocal(): TAITime  = taiTimeAt(localZoneId)
  def taiTimeAtHawaii(): TAITime = taiTimeAt(hawaiiZoneId)
  def taiTimeAt(zoneId: ZoneId): TAITime

  def toUTC(taiInstant: TAITime): UTCTime
  def toTAI(utcInstant: UTCTime): TAITime

  def scheduleOnce(startTime: TAITime)(task: Runnable): Cancellable

  def schedulePeriodically(duration: Duration)(task: Runnable): Cancellable
  def schedulePeriodically(startTime: TAITime, duration: Duration)(task: Runnable): Cancellable

  private[time] def setTaiOffset(offset: Int): Unit
}
