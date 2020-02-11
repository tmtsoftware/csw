package csw.services

import csw.services.utils.ResourceReader

class Redis(settings: Settings) {
  import settings._
  private val eventMasterConf: String = ResourceReader.copyToTmp("event/master.conf").getAbsolutePath
  private val alarmMasterConf: String = ResourceReader.copyToTmp("alarm/master.conf").getAbsolutePath

  def startRedis(port: String, conf: String): Process =
    new ProcessBuilder("redis-server", conf, "--port", port).inheritIO().start()

  def startEvent(): Process = startRedis(eventPort, eventMasterConf)
  def startAlarm(): Process = startRedis(alarmPort, alarmMasterConf)
}
