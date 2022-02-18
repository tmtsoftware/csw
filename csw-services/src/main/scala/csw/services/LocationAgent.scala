package csw.services

import csw.location.agent.wiring.Wiring
import csw.location.agent.{Main => LocationAgentMain}
import csw.services.internal.FutureExt._
import csw.services.internal.{ManagedService, Settings}
import csw.services.utils.{ColoredConsole, ResourceReader}

import scala.collection.mutable
import scala.util.control.NonFatal

class LocationAgent(settings: Settings) {
  private val databaseServiceName = "Database Service"
  private val sentinelServiceName = "Redis Sentinel"

  import settings._
  private val pgHbaConf: String = ResourceReader.copyToTmp("database/pg_hba.conf").getAbsolutePath

  def databaseService(enable: Boolean): ManagedService[Option[(Process, Wiring)], Unit] =
    ManagedService(
      databaseServiceName,
      enable,
      () => startPostgres(),
      killAgent
    )

  def sentinelService(event: Boolean, alarm: Boolean): ManagedService[Option[(Process, Wiring)], Unit] =
    ManagedService(
      sentinelServiceName,
      event || alarm,
      () => startSentinel(event, alarm),
      killAgent
    )

  private def startAgent(args: Array[String]): Option[(Process, Wiring)] = LocationAgentMain.start(args)

  private def startPostgres(): Option[(Process, Wiring)] =
    try {
      startAgent(
        Array(
          "--prefix",
          "CSW.DatabaseServer",
          "--command",
          s"postgres --hba_file=$pgHbaConf --unix_socket_directories=$dbUnixSocketDirs -i -p $dbPort",
          "--port",
          dbPort
        )
      )
    }
    catch {
      case NonFatal(e) =>
        ColoredConsole.RED.println(
          "Make sure 'PGDATA' env variable should point to a location under your HOME dir, not the one used by the system postgres installation, which has protected permissions. It is recommended to create a data directory especially for use with the Database Service. e.g. for linux users: /home/username/postgres and for mac users: /usr/local/var/postgres"
        )
        throw e
    }

  private val killAgent: Option[(Process, Wiring)] => Unit =
    _.foreach { case (process, wiring) =>
      process.destroyForcibly(); wiring.actorRuntime.shutdown().await()
    }

  private val sentinelConf: String =
    ResourceReader
      .copyToTmp(
        "redis/sentinel.conf",
        transform = _.replace("eventServer 127.0.0.1", s"eventServer $hostName")
          .replace("alarmServer 127.0.0.1", s"alarmServer $hostName")
      )
      .getAbsolutePath

  private def startSentinel(event: Boolean, alarm: Boolean): Option[(Process, Wiring)] = {
    Redis.requireRedisInstalled()
    val prefixes: mutable.Buffer[String] = mutable.Buffer.empty
    if (event) prefixes.addOne("CSW.EventServer")
    if (alarm) prefixes.addOne("CSW.AlarmServer")
    if (alarm || event) {
      startAgent(
        Array(
          "--prefix",
          prefixes.mkString(","),
          "--command",
          s"redis-sentinel $sentinelConf --port $sentinelPort",
          "--port",
          sentinelPort
        )
      )
    }
    else throw new IllegalArgumentException("Either event flag or alarm flag needs to be true")
  }

}
