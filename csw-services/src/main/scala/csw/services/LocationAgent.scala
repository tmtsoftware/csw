package csw.services

import csw.location.agent.{Main => LocationAgentMain}
import csw.services.internal.Settings
import csw.services.utils.ResourceReader

import scala.collection.mutable

class LocationAgent(settings: Settings) {
  import settings._
  private val pgHbaConf: String = ResourceReader.copyToTmp("database/pg_hba.conf").getAbsolutePath

  def start(name: String, args: Array[String]): Unit = Service.start(name, LocationAgentMain.main(args))

  def startPostgres(): Unit = {
    start(
      "Database Service",
      Array(
        "--prefix",
        "CSW.DatabaseServer",
        "--command",
        s"postgres --hba_file=$pgHbaConf --unix_socket_directories=$dbUnixSocketDirs -i -p $dbPort"
      )
    )
  }

  private val sentinelConf: String =
    ResourceReader
      .copyToTmp(
        "redis/sentinel.conf",
        transform = _.replace("eventServer 127.0.0.1", s"eventServer $hostName")
          .replace("alarmServer 127.0.0.1", s"alarmServer $hostName")
      )
      .getAbsolutePath

  def startSentinel(event: Boolean, alarm: Boolean): Unit = {
    val prefixes: mutable.Buffer[String] = mutable.Buffer.empty
    if (event) prefixes.addOne("CSW.EventServer")
    if (alarm) prefixes.addOne("CSW.AlarmServer")
    if (alarm || event)
      start(
        "Redis Sentinel",
        Array(
          "--prefix",
          prefixes.mkString(","),
          "--command",
          s"redis-sentinel $sentinelConf --port $sentinelPort"
        )
      )
  }

}
