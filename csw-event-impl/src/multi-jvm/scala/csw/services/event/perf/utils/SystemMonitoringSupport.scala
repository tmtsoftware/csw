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

  val perfJavaFlamesPath = "$$HOME/TMT/perf-map-agent/bin/perf-java-flames"
  val topResultsPath     = s"$$HOME/perf/top_${Instant.now()}.log"
  val jstatResultsPath   = s"$$HOME/perf/jstat_${pid}_${Instant.now()}.log"
  val cpuUsagePath       = s"$$HOME/perf/cpu_usage_${Instant.now()}.log"
  val cpuUsageGraphPath  = s"$$HOME/perf/cpu_usage_plot_${Instant.now()}.png"

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

  val plotCpuUsageGraphScript: String =
    s"""
       |#!/usr/bin/env bash
       |
       |timestamp=`date +%F_%H-%M-%S`
       |
       |if [ ! -f $topResultsPath ]; then
       |  exit 1
       |fi
       |
       |echo "Extracting CPU usage from [ $topResultsPath ]"
       |awk '/CPU/ {printf ("%s\\t%s\\t%s\\t%s\\n", i++, $$3, $$5, $$7)}'  $topResultsPath >> $cpuUsagePath
       |
       |echo "Adding headers in [$cpuUsagePath]"
       |sed -i 1i"seconds\\tUser\\tSystem\\tIdle" $cpuUsagePath
       |
       |echo "==========================================="
       |echo "Plotting CPU usage graph using gnuplot ..."
       |echo "==========================================="
       |gnuplot <<-EOFMarker
       |    set xlabel "Seconds"
       |    set ylabel "% CPU Usage"
       |    set title "CPU Usage"
       |    set term pngcairo size 1680,1050 enhanced font 'Verdana,18'
       |    set output "$cpuUsageGraphPath"
       |    plot for [col=2:4] "$cpuUsagePath" using 1:col with lines title columnheader
       |EOFMarker
       |
       |echo "CPU usage graph plotted and saved at [$cpuUsageGraphPath]"
       |
     """.stripMargin

  def runTop(): process.Process = {
    Thread.sleep(Random.nextInt(2) * 1000)

    val topCommand = createExecutableScript("top", topScript)
    executeCmd(topCommand)
  }

  def runJstat(): process.Process = {
    val jstatScriptPath = createExecutableScript("jstat", jstatScript)
    executeCmd(s"$jstatScriptPath $pid")
  }

  def plotCpuUsageGraph(): process.Process = {
    val plotCmd = createExecutableScript("cpu_plot", plotCpuUsageGraphScript)
    executeCmd(plotCmd)
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

  private def createExecutableScript(name: String, content: String): String = {
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
