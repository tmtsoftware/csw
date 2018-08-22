package csw.services.alarm.client.internal.commons
import com.typesafe.config.Config

class Settings(config: Config) {

  private val alarmConfig = config.getConfig("csw-alarm")

  val masterId: String            = alarmConfig.getString("redis.masterId")
  val refreshInSeconds: Int       = alarmConfig.getInt("refresh-in-seconds") // default value is 3 seconds
  val maxMissedRefreshCounts: Int = alarmConfig.getInt("max-missed-refresh-counts") //default value is 3 times
  val shelveTimeoutHourOfDay: Int = alarmConfig.getInt("shelve-timeout-hour-of-day") //default value is 8
  val ttlInSeconds: Int           = refreshInSeconds * maxMissedRefreshCounts

}
