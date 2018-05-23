package csw.services.event.perf.utils

import java.io.File
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

  val homeDir: String        = System.getProperty("user.home")
  val perfScriptsDir: String = System.getProperty("user.dir") + "/scripts/perf"
  val perfJavaFlamesPath     = s"$homeDir/TMT/perf-map-agent/bin/perf-java-flames"
  val topResultsPath         = s"$homeDir/perf/top_${Instant.now()}.log"

  def runTop(): Option[process.Process] = {
    Thread.sleep(Random.nextInt(2) * 1000)
    val process = executeCmd(s"$perfScriptsDir/top.sh $topResultsPath")
    Thread.sleep(1000)
    if (process.isAlive()) Some(process)
    else None
  }

  def runJstat(): process.Process = executeCmd(s"$perfScriptsDir/jstat.sh $pid ${myself.name}")

  def plotCpuUsageGraph(): process.Process                          = executeCmd(s"$perfScriptsDir/cpu_plot.sh $topResultsPath")
  def plotMemoryUsageGraph(): process.Process                       = executeCmd(s"$perfScriptsDir/memory_plot.sh $topResultsPath")
  def plotLatencyHistogram(inputFilesPath: String): process.Process = executeCmd(s"$perfScriptsDir/hist_plot.sh $inputFilesPath")

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

  private def executeCmd(cmd: String): process.Process = {
    cmd.run(new ProcessLogger {
      override def buffer[T](f: ⇒ T): T = f

      override def out(s: ⇒ String): Unit = println(s"[perf @ $myself($pid)][OUT] " + s)

      override def err(s: ⇒ String): Unit = println(s"[perf @ $myself($pid)][ERR] " + s)
    })
  }

}
