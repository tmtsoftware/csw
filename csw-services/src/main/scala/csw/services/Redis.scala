/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.services

import csw.services.internal.{ManagedService, Settings}
import csw.services.utils.{ColoredConsole, ResourceReader}

import scala.util.control.NonFatal

class Redis(settings: Settings) {
  import settings._
  private val eventMasterConf: String = updatedRedisConf(eventPort, "event_master.pid", "dump_event_master.rdb")
  private val alarmMasterConf: String = updatedRedisConf(alarmPort, "alarm_master.pid", "dump_alarm_master.rdb", "\"Kg$x\"")

  def eventService(enable: Boolean): ManagedService[Process, Unit] =
    redisService("Event Service", enable, eventPort, eventMasterConf)
  def alarmService(enable: Boolean): ManagedService[Process, Unit] =
    redisService("Alarm Service", enable, alarmPort, alarmMasterConf)

  private def startRedis(port: String, conf: String) = {
    Redis.requireRedisInstalled()
    new ProcessBuilder("redis-server", conf, "--port", port).inheritIO().start()
  }

  private def redisService(name: String, enable: Boolean, port: String, conf: String): ManagedService[Process, Unit] =
    ManagedService[Process, Unit](
      name,
      enable,
      () => startRedis(port, conf),
      _.destroyForcibly()
    )

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

object Redis {
  private val sentinel        = "redis-sentinel"
  private val minRedisVersion = 4

  def requireRedisInstalled(): Int =
    try {
      ColoredConsole.GREEN.println("Checking for Redis installation ...")
      val versionOutput = os.proc(sentinel, "--version").call().out.lines().head
      val rawVersion    = versionOutput.split(" ").find(_.startsWith("v=")).map(_.replace("v=", "")).get
      val majorVersion  = rawVersion.split('.').head.toInt
      require(
        majorVersion >= minRedisVersion,
        s"Required Redis majorVersion is [$minRedisVersion], but only majorVersion [$majorVersion] was found"
      )
      ColoredConsole.GREEN.println(s"Redis is installed with version [$rawVersion]")
      majorVersion
    }
    catch {
      case NonFatal(e) =>
        ColoredConsole.RED.println(s"Redis installation check failed, error: ${e.getMessage}")
        throw e
    }
}
