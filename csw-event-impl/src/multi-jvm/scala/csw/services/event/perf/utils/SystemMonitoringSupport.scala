package csw.services.event.perf.utils

import java.io.File
import java.nio.file.Files
import java.time.Instant

import akka.remote.testconductor.RoleName
import akka.remote.testkit.MultiNodeSpec

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.sys.process
import scala.sys.process.{stringToProcess, ProcessLogger}
import scala.util.Random

trait SystemMonitoringSupport { _: MultiNodeSpec ⇒

  lazy val pid: Int = {
    import java.lang.management._
    val name = ManagementFactory.getRuntimeMXBean.getName
    name.substring(0, name.indexOf('@')).toInt
  }

  val perfJavaFlamesPath = "/Users/pritamkadam/TMT/perf-map-agent/bin/perf-java-flames"
  val topResultsPath     = s"~/perf/top_${Instant.now()}.log"
  val jstatResultsPath   = s"~/perf/jstat_${pid}_${Instant.now()}.log"

  val topScript: String =
    s"""
      |#!/usr/bin/env bash
      |
      |pname=$$(basename $$0)
      |
      |existing_pid=`pgrep -fl "top.*.sh" | grep -v $${pname}`
      |exist=$$?
      |
      |if [[ $${exist} != "0" ]]; then
      |    touch $topResultsPath
      |    echo "[Info] Running top command with interval of 1 second ..."
      |    echo "[Info] Saving Results of top in $topResultsPath"
      |    while true
      |    do
      |        top | grep -m10 "" >> $topResultsPath
      |        sleep 1
      |    done
      |else
      |    echo "======================================================================"
      |    echo "[Warning] Top is already running on this node with pid=$$existing_pid ."
      |    echo "======================================================================"
      |fi
      |
    """.stripMargin

  val jstatScript: String =
    s"""
      |#!/usr/bin/env bash
      |
      |touch $jstatResultsPath
      |
      |echo "[Info] Running jstat script: [ $$0 ]"
      |echo "[Info] Saving Results of jstat in $jstatResultsPath"
      |
      |nohup jstat -gc $pid 1s >> $jstatResultsPath &
      |
    """.stripMargin

  def runTop(): process.Process = {
    Thread.sleep(Random.nextInt(2) * 1000)

    val topCommand = createExecutableShellScript("top", topScript)
    executeCmd(topCommand)
  }

  def runJstat(): process.Process = {
    val jstatScriptPath = createExecutableShellScript("jstat", jstatScript)
    executeCmd(s"$jstatScriptPath $pid")
  }

  /**
   * Runs `perf-java-flames` script on given node (JVM process).
   * Refer to https://github.com/jrudolph/perf-map-agent for options and manual.
   *
   * Options are currently to be passed in via `export PERF_MAP_OPTIONS` etc.
   */
  def runPerfFlames(nodes: RoleName*)(delay: FiniteDuration, time: FiniteDuration = 15.seconds): Unit = {
    if (isPerfJavaFlamesAvailable && isNode(nodes: _*)) {
      import scala.concurrent.ExecutionContext.Implicits.global

      val afterDelay = akka.pattern.after(delay, system.scheduler)(Future.successful("GO!"))
      afterDelay onComplete { it ⇒
        val perfCommand = s"$perfJavaFlamesPath $pid"
        println(s"[perf @ $myself($pid)][OUT]: " + perfCommand)

        executeCmd(perfCommand)
      }
    }
  }

  def isPerfJavaFlamesAvailable: Boolean = {
    val isIt = new File(perfJavaFlamesPath).exists()
    if (!isIt) println(s"[Warning] perf-java-flames not available under [$perfJavaFlamesPath]! Skipping perf profiling.")
    isIt
  }

  private def createExecutableShellScript(name: String, content: String): String = {
    val topTmpPath = Files.createTempFile(name, ".sh")
    val topTmpFile = topTmpPath.toFile
    topTmpFile.deleteOnExit()
    topTmpFile.setExecutable(true)

    Files.write(topTmpPath, content.getBytes).toString
  }

  private def executeCmd(cmd: String): process.Process = {
    cmd.run(new ProcessLogger {
      override def buffer[T](f: ⇒ T): T = f

      override def out(s: ⇒ String): Unit = println(s"[perf @ $myself($pid)][OUT] " + s)

      override def err(s: ⇒ String): Unit = println(s"[perf @ $myself($pid)][ERR] " + s)
    })
  }

}
