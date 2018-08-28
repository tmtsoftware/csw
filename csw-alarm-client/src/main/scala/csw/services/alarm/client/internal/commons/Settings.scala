package csw.services.alarm.client.internal.commons
import com.typesafe.config.Config

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.duration.FiniteDuration

class Settings(config: Config) {

  private val alarmConfig = config.getConfig("csw-alarm")

  val masterId: String                = alarmConfig.getString("redis.masterId")
  val refreshInterval: FiniteDuration = alarmConfig.getDuration("refresh-interval").toScala // default value is 3 seconds
  val maxMissedRefreshCounts: Int     = alarmConfig.getInt("max-missed-refresh-counts") //default value is 3 times
  val shelveTimeoutHourOfDay: Int     = alarmConfig.getInt("shelve-timeout-hour-of-day") //default value is 8
  val ttlInSeconds: Long              = refreshInterval.toSeconds * maxMissedRefreshCounts

}
