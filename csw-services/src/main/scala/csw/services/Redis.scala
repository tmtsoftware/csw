package csw.services

import csw.services.internal.Settings
import csw.services.utils.ResourceReader

class Redis(settings: Settings) {
  import settings._
  private val eventMasterConf: String = updatedRedisConf(eventPort, "event_master.pid", "dump_event_master.rdb")
  private val alarmMasterConf: String = updatedRedisConf(alarmPort, "alarm_master.pid", "dump_alarm_master.rdb", "\"Kg$x\"")

  def startRedis(name: String, port: String, conf: String): Process =
    Service.start(name, new ProcessBuilder("redis-server", conf, "--port", port).inheritIO().start())

  def startEvent(): Process = startRedis("Event Service", eventPort, eventMasterConf)
  def startAlarm(): Process = startRedis("Alarm Service", alarmPort, alarmMasterConf)

  private def replacePid(name: String): String => String =
    _.replace("pidfile /var/run/redis_6379.pid", s"pidfile /var/run/$name")
  private def replacePort(port: String): String => String   = _.replace("port 6379", s"port $port")
  private def replaceDbName(name: String): String => String = _.replace("dbfilename dump.rdb", s"dbfilename $name")
  private def replaceKeyspace(name: String): String => String =
    _.replace("notify-keyspace-events \"\"", s"notify-keyspace-events $name")

  private def updatedRedisConf(port: String, pid: String, dbName: String, keyspaceEvent: String = "\"\""): String =
    ResourceReader
      .copyToTmp(
        "redis/master.conf",
        suffix = ".conf",
        transform = replacePort(port) andThen replacePid(pid) andThen replaceDbName(dbName) andThen replaceKeyspace(keyspaceEvent)
      )
      .getAbsolutePath

}
